package com.delmoralcristian.notifier.application.service;

import com.delmoralcristian.notifier.advice.TrackProcessingTime;
import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.application.dto.PagedResponse;
import com.delmoralcristian.notifier.application.port.in.NotificationEventUseCase;
import com.delmoralcristian.notifier.application.port.out.ClientPersistencePort;
import com.delmoralcristian.notifier.application.port.out.NotificationEventPersistencePort;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import com.delmoralcristian.notifier.infrastructure.adapter.out.mapper.NotificationEventMapper;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationEventService implements NotificationEventUseCase {

    private final DeliveryService deliveryService;
    private final NotificationEventPersistencePort notificationAdapter;
    private final ClientPersistencePort clientAdapter;
    private final NotificationEventMapper notificationEventMapper;

    @TrackProcessingTime
    @Cacheable(value = "event-pages",
        key = "#clientId + ':' + #status + ':' + #from + ':' + #to + ':' + #page + ':' + #size")
    @Override
    public PagedResponse<NotificationEventDTO> findByFilters(
        String clientId,
        ENotificationStatus status,
        LocalDateTime from,
        LocalDateTime to,
        int page,
        int size) {

        if (clientAdapter.findById(clientId).isEmpty()) {
            throw new EntityNotFoundException("Client not found with ID: " + clientId);
        }

        if (from != null && to != null && (to.isBefore(from) || to.isEqual(from))) {
            throw new IllegalArgumentException("'to' datetime must be after 'from' datetime");
        }

        log.info("Finding notification events: clientId={}, status={}, from={}, to={}, page={}, size={}",
            clientId, status, from, to, page, size);

        var statusName = status != null ? status.name() : null;
        var pageable = PageRequest.of(page, size);
        var result = notificationAdapter.findByFilters(clientId, statusName, from, to, pageable);

        return new PagedResponse<>(
            result.getContent().stream().map(notificationEventMapper::transformToDto).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @TrackProcessingTime
    @Cacheable(value = "events", key = "#eventId")
    @Override
    public NotificationEventDTO getByEventId(String eventId) {
        log.info("Finding notification event by eventId: {}", eventId);
        var event = notificationAdapter.findByEventId(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Notification event not found for eventId: " + eventId));
        return notificationEventMapper.transformToDto(event);
    }

    @TrackProcessingTime
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "events", key = "#eventId"),
        @CacheEvict(value = "event-pages", allEntries = true)
    })
    @Override
    public void replayNotification(String eventId) {
        log.info("Replaying notification event with eventId: {}", eventId);

        var event = notificationAdapter.findByEventId(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Notification event not found for eventId: " + eventId));

        if ("COMPLETED".equals(event.getDeliveryStatus())) {
            throw new IllegalArgumentException("Event " + eventId + " is already COMPLETED and cannot be replayed");
        }

        deliveryService.reSend(event);
    }
}
