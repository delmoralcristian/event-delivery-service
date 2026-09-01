package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.repository;

import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationEventRepositoryCustom {

    Page<NotificationEventEntity> findByFilters(
        String clientId,
        String status,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );
}
