package com.delmoralcristian.notifier.application.service;

import static com.delmoralcristian.notifier.enums.ENotificationStatus.COMPLETED;

import com.delmoralcristian.notifier.application.port.in.DeliveryServiceUseCase;
import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.exceptions.EntityNotFoundException;
import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;
import com.delmoralcristian.notifier.infrastructure.adapter.out.mapper.NotificationEventMapper;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryService implements DeliveryServiceUseCase {

    private final NotificationEventPersistencePort notificationAdapter;
    private final ClientPersistencePort clientAdapter;
    private final NotificationEventMapper notificationEventMapper;
    private final DeliveryRetryHandler deliveryRetryHandler;

    @Override
    public void send(EventDTO eventDTO) {
        var eventId = eventDTO.getEventId();

        if (this.notificationAdapter.existsByEventIdAndClientId(eventId, eventDTO.getClientId())) {
            log.warn("Event {} for client {} already processed. Skipping delivery.", eventId, eventDTO.getClientId());
            return;
        }

        var client = this.getClient(eventDTO.getClientId());
        var event = this.notificationEventMapper.transformToNotificationEvent(
            client.getId(), client.getWebhookUrl(), eventDTO);

        this.deliveryRetryHandler.attemptDelivery(event);

        this.notificationAdapter.save(event);
        log.info("Event {} processed with delivery status {}", eventId, event.getDeliveryStatus());
    }

    @Override
    public void reSend(String eventId) {
        var event = notificationAdapter.findByEventId(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Notification event not found for eventId: " + eventId));

        if (COMPLETED.name().equals(event.getDeliveryStatus())) {
            throw new IllegalArgumentException("Event " + eventId + " is already COMPLETED and cannot be replayed");
        }

        this.deliveryRetryHandler.attemptDelivery(event);
        log.info("Event {} reprocessed with delivery status {}", eventId, event.getDeliveryStatus());
        this.notificationAdapter.save(event);
    }

    private ClientEntity getClient(String clientId) {
        return this.clientAdapter.findById(clientId)
            .orElseThrow(() -> new EntityNotFoundException("Client not found with ID: " + clientId));
    }
}
