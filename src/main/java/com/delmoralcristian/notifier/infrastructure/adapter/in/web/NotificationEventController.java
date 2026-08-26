package com.delmoralcristian.notifier.infrastructure.adapter.in.web;

import com.delmoralcristian.notifier.application.dto.NotificationEventDTO;
import com.delmoralcristian.notifier.application.dto.PagedResponse;
import com.delmoralcristian.notifier.application.service.NotificationEventService;
import com.delmoralcristian.notifier.enums.ENotificationStatus;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping(value = "/notification_events")
@RequiredArgsConstructor
public class NotificationEventController {

    private final NotificationEventService notificationEventService;

    @GetMapping
    public ResponseEntity<PagedResponse<NotificationEventDTO>> getAll(
        @NotBlank(message = "clientId is required") @RequestParam String clientId,
        @RequestParam(required = false) ENotificationStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(this.notificationEventService.findByFilters(clientId, status, from, to, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationEventDTO> getByEventId(@PathVariable String id) {
        return ResponseEntity.ok(this.notificationEventService.getByEventId(id));
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<Void> replay(@PathVariable String id) {
        this.notificationEventService.replayNotification(id);
        return ResponseEntity.accepted().build();
    }
}
