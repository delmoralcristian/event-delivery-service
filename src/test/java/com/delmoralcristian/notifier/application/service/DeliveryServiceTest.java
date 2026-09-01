package com.delmoralcristian.notifier.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.exceptions.EntityNotFoundException;
import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;
import com.delmoralcristian.notifier.infrastructure.adapter.out.mapper.NotificationEventMapper;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
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
    private static final String WEBHOOK_URL = "https://webhook.example.com";

    @Test
    void send_newEvent_processesAndSaves() {
        var eventDTO = buildEventDTO();
        var client = buildClient();
        var entity = buildEntity("PENDING");
        when(notificationAdapter.existsByEventIdAndClientId(EVENT_ID, CLIENT_ID)).thenReturn(false);
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(notificationEventMapper.transformToNotificationEvent(CLIENT_ID, WEBHOOK_URL, eventDTO)).thenReturn(entity);

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
        var entity = buildEntity("FAILED");
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));

        deliveryService.reSend(EVENT_ID);

        verify(deliveryRetryHandler).attemptDelivery(entity);
        verify(notificationAdapter).save(entity);
    }

    @Test
    void reSend_alreadyCompleted_throwsIllegalArgument() {
        var entity = buildEntity("COMPLETED");
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> deliveryService.reSend(EVENT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already COMPLETED");
    }

    @Test
    void reSend_notFound_throwsEntityNotFoundException() {
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.reSend(EVENT_ID))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(EVENT_ID);
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
            .webhookUrl(WEBHOOK_URL)
            .active(true)
            .build();
    }

    private NotificationEventEntity buildEntity(String status) {
        return NotificationEventEntity.builder()
            .eventId(EVENT_ID)
            .eventType("credit_card_payment")
            .content("Payment of $150.00")
            .deliveryDate(LocalDateTime.now())
            .deliveryStatus(status)
            .clientId(CLIENT_ID)
            .webhookUrl(WEBHOOK_URL)
            .build();
    }

    private <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
