package com.delmoralcristian.notifier.application.dto;

import com.delmoralcristian.notifier.enums.EEventType;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

public record NotificationEventDTO(
    String eventId,
    EEventType eventType,
    String content,
    LocalDateTime deliveryDate,
    ENotificationStatus deliveryStatus
) implements Serializable {

}
