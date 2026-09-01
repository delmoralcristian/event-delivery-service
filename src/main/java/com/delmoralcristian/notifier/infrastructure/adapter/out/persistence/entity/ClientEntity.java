package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEntity {

    @Id
    @Column("id")
    private String id;

    @Column("name")
    private String name;

    @Column("webhook_url")
    private String webhookUrl;

    @Column("active")
    private boolean active;

    @Column("api_key")
    private String apiKey;
}
