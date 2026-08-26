package com.delmoralcristian.notifier.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.application.dto.PagedResponse;
import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.enums.EEventType;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import com.delmoralcristian.notifier.infrastructure.adapter.out.mapper.NotificationEventMapper;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock
    private NotificationEventPersistencePort notificationAdapter;
    @Mock
    private ClientPersistencePort clientAdapter;
    @Mock
    private NotificationEventMapper notificationEventMapper;
    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private NotificationEventService service;

    private static final String CLIENT_ID = "CLIENT001";
    private static final String EVENT_ID = "EVT001";
    private static final LocalDateTime FROM = LocalDateTime.of(2024, 3, 15, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2024, 3, 15, 23, 59);

    @Test
    void findByFilters_returnsEvents() {
        var entity = buildEntity(EVENT_ID, "COMPLETED");
        var dto = buildDto(EVENT_ID, ENotificationStatus.COMPLETED);
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.of(buildClient()));
        when(notificationAdapter.findByFilters(eq(CLIENT_ID), eq("COMPLETED"), eq(FROM), eq(TO), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));
        when(notificationEventMapper.transformToDto(entity)).thenReturn(dto);

        PagedResponse<NotificationEventDTO> result = service.findByFilters(CLIENT_ID, ENotificationStatus.COMPLETED, FROM, TO, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).eventId()).isEqualTo(EVENT_ID);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void findByFilters_withNullDates_doesNotThrow() {
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.of(buildClient()));
        when(notificationAdapter.findByFilters(eq(CLIENT_ID), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        var result = service.findByFilters(CLIENT_ID, null, null, null, 0, 20);

        assertThat(result.content()).isEmpty();
    }

    @Test
    void findByFilters_clientNotFound_throwsEntityNotFoundException() {
        when(clientAdapter.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByFilters("UNKNOWN", null, null, null, 0, 20))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("UNKNOWN");
    }

    @Test
    void findByFilters_invalidDateRange_throwsIllegalArgument() {
        when(clientAdapter.findById(CLIENT_ID)).thenReturn(Optional.of(buildClient()));

        assertThatThrownBy(() -> service.findByFilters(CLIENT_ID, null, TO, FROM, 0, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("'to' datetime must be after 'from'");
    }

    @Test
    void getByEventId_returnsDto() {
        var entity = buildEntity(EVENT_ID, "COMPLETED");
        var dto = buildDto(EVENT_ID, ENotificationStatus.COMPLETED);
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));
        when(notificationEventMapper.transformToDto(entity)).thenReturn(dto);

        var result = service.getByEventId(EVENT_ID);

        assertThat(result.eventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void getByEventId_notFound_throwsEntityNotFoundException() {
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEventId(EVENT_ID))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(EVENT_ID);
    }

    @Test
    void replayNotification_delegatesToDeliveryService() {
        var entity = buildEntity(EVENT_ID, "FAILED");
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));

        service.replayNotification(EVENT_ID);

        verify(deliveryService).reSend(entity);
    }

    @Test
    void replayNotification_notFound_throwsEntityNotFoundException() {
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replayNotification(EVENT_ID))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining(EVENT_ID);
    }

    @Test
    void replayNotification_alreadyCompleted_throwsIllegalArgument() {
        var entity = buildEntity(EVENT_ID, "COMPLETED");
        when(notificationAdapter.findByEventId(EVENT_ID)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.replayNotification(EVENT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already COMPLETED");
    }

    private NotificationEventEntity buildEntity(String eventId, String status) {
        var client = ClientEntity.builder()
            .id(CLIENT_ID)
            .webhookUrl("https://webhook.example.com")
            .build();
        return NotificationEventEntity.builder()
            .eventId(eventId)
            .eventType("CREDIT_CARD_PAYMENT")
            .content("Payment content")
            .deliveryDate(LocalDateTime.now())
            .deliveryStatus(status)
            .client(client)
            .build();
    }

    private NotificationEventDTO buildDto(String eventId, ENotificationStatus status) {
        return new NotificationEventDTO(eventId, EEventType.CREDIT_CARD_PAYMENT, "Payment content",
            LocalDateTime.now(), status);
    }

    private ClientEntity buildClient() {
        return ClientEntity.builder()
            .id(CLIENT_ID)
            .name("Client A")
            .webhookUrl("https://webhook.example.com")
            .active(true)
            .build();
    }
}
