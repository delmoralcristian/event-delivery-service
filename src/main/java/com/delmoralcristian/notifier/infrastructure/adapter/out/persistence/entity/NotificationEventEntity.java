package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("notification_event")
@EqualsAndHashCode(of = {"id"})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventEntity {

    @Id
    @Column("id")
    private Long id;

    @Column("event_id")
    private String eventId;

    @Column("event_type")
    private String eventType;

    @Column("content")
    private String content;

    @Column("delivery_date")
    private LocalDateTime deliveryDate;

    @Column("delivery_status")
    private String deliveryStatus;

    @Column("client_id")
    private String clientId;

    @Column("webhook_url")
    private String webhookUrl;
}
