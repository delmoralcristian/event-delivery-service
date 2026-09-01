package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.adapter;

import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository.JpaNotificationEventRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationEventPersistenceAdapter implements NotificationEventPersistencePort {

    private final JpaNotificationEventRepository repository;

    @Override
    public Optional<NotificationEventEntity> findByEventId(String eventId) {
        return this.repository.findByEventId(eventId);
    }

    @Override
    public boolean existsByEventIdAndClientId(String eventId, String clientId) {
        return this.repository.existsByEventIdAndClientId(eventId, clientId);
    }

    @Override
    public Page<NotificationEventEntity> findByFilters(
        String clientId, String deliveryStatus, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return this.repository.findByFilters(clientId, deliveryStatus, from, to, pageable);
    }

    @Override
    @Transactional
    public NotificationEventEntity save(NotificationEventEntity notificationEventEntity) {
        return this.repository.save(notificationEventEntity);
    }
}
