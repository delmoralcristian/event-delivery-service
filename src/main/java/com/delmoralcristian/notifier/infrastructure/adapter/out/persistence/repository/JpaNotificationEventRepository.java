package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaNotificationEventRepository extends CrudRepository<NotificationEventEntity, Long> {

    Optional<NotificationEventEntity> findByEventId(String eventId);

    boolean existsByEventIdAndClientId(String eventId, String clientId);

    @Query("SELECT e FROM NotificationEventEntity e WHERE e.client.id = :clientId " +
           "AND (:status IS NULL OR e.deliveryStatus = :status) " +
           "AND (:from IS NULL OR e.deliveryDate >= :from) " +
           "AND (:to IS NULL OR e.deliveryDate <= :to)")
    Page<NotificationEventEntity> findByFilters(
        @Param("clientId") String clientId,
        @Param("status") String status,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

}
