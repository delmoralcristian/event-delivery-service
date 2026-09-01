package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNotificationEventRepository
    extends CrudRepository<NotificationEventEntity, Long>, NotificationEventRepositoryCustom {

    Optional<NotificationEventEntity> findByEventId(String eventId);

    boolean existsByEventIdAndClientId(String eventId, String clientId);
}
