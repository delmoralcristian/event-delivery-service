package com.delmoralcristian.notifier.application.port.in;

import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.application.dto.PagedResponse;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import java.time.LocalDateTime;

public interface NotificationEventUseCase {

    PagedResponse<NotificationEventDTO> findByFilters(String clientId, ENotificationStatus status,
        LocalDateTime from, LocalDateTime to, int page, int size);

    NotificationEventDTO getByEventId(String eventId, String clientId);

    void replayNotification(String eventId, String clientId);
}
