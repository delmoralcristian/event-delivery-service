package com.delmoralcristian.notifier.application.port.out;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationEventPersistencePort {

    Optional<NotificationEventEntity> findByEventId(String eventId);

    boolean existsByEventIdAndClientId(String eventId, String clientId);

    Page<NotificationEventEntity> findByFilters(
        String clientId,
        String deliveryStatus,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    NotificationEventEntity save(NotificationEventEntity notificationEventEntity);
}
