package com.delmoralcristian.notifier.infrastructure.adapter.out.mapper;

import static com.delmoralcristian.notifier.enums.ENotificationStatus.COMPLETED;

import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.enums.EEventType;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import com.delmoralcristian.notifier.infrastructure.adapter.in.consumer.EventDTO;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationEventMapper {

    default NotificationEventDTO transformToDto(NotificationEventEntity event) {
        return new NotificationEventDTO(
            event.getEventId(),
            EEventType.fromString(event.getEventType()),
            event.getContent(),
            event.getDeliveryDate(),
            ENotificationStatus.valueOf(event.getDeliveryStatus())
        );
    }

    default NotificationEventEntity transformToNotificationEvent(String clientId, String webhookUrl, EventDTO eventDTO) {
        return NotificationEventEntity.builder()
            .eventId(eventDTO.getEventId())
            .eventType(eventDTO.getEventType())
            .content(eventDTO.getContent())
            .clientId(clientId)
            .webhookUrl(webhookUrl)
            .deliveryDate(LocalDateTime.now())
            .deliveryStatus(COMPLETED.name())
            .build();
    }
}
