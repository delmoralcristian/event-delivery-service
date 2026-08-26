package com.delmoralcristian.notifier.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;
import com.delmoralcristian.notifier.infrastructure.adapter.out.mapper.NotificationEventMapper;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private NotificationEventPersistencePort notificationAdapter;
    @Mock
    private ClientPersistencePort clientAdapter;
    @Mock
    private NotificationEventMapper notificationEventMapper;
    @Mock
    private DeliveryRetryHandler deliveryRetryHandler;

    @InjectMocks
    private DeliveryService deliveryService;

    private static final String EVENT_ID = "EVT001";
    private static final String CLIENT_ID = "CLIENT001";

    @Test
    void send_newEvent_processesAndSaves() {
        var eventDTO = buildEventDTO();
        var client = buildClient();
        var entity = buildEntity();
        when(notificationAdapter.existsByEventIdAndClientId(EVENT_ID, CLIENT_ID)).thenReturn(false);
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(notificationEventMapper.transformToNotificationEvent(client, eventDTO)).thenReturn(entity);

        deliveryService.send(eventDTO);

        verify(deliveryRetryHandler).attemptDelivery(entity);
        verify(notificationAdapter).save(entity);
    }

    @Test
    void send_duplicateEvent_skipsDelivery() {
        var eventDTO = buildEventDTO();
        when(notificationAdapter.existsByEventIdAndClientId(EVENT_ID, CLIENT_ID)).thenReturn(true);

        deliveryService.send(eventDTO);

        verify(deliveryRetryHandler, never()).attemptDelivery(any());
        verify(notificationAdapter, never()).save(any());
    }

    @Test
    void send_clientNotFound_throwsEntityNotFoundException() {
        var eventDTO = buildEventDTO();
        when(notificationAdapter.existsByEventIdAndClientId(EVENT_ID, CLIENT_ID)).thenReturn(false);
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.send(eventDTO))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(CLIENT_ID);
    }

    @Test
    void reSend_attemptsDeliveryAndSaves() {
        var entity = buildEntity();

        deliveryService.reSend(entity);

        verify(deliveryRetryHandler).attemptDelivery(entity);
        verify(notificationAdapter).save(entity);
    }

    private EventDTO buildEventDTO() {
        return EventDTO.builder()
            .eventId(EVENT_ID)
            .clientId(CLIENT_ID)
            .eventType("credit_card_payment")
            .content("Payment of $150.00")
            .build();
    }

    private ClientEntity buildClient() {
        return ClientEntity.builder()
            .id(CLIENT_ID)
            .name("Client A")
            .webhookUrl("https://webhook.example.com")
            .active(true)
            .build();
    }

    private NotificationEventEntity buildEntity() {
        return NotificationEventEntity.builder()
            .eventId(EVENT_ID)
            .eventType("credit_card_payment")
            .content("Payment of $150.00")
            .deliveryDate(LocalDateTime.now())
            .deliveryStatus("COMPLETED")
            .client(buildClient())
            .build();
    }

    private <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
