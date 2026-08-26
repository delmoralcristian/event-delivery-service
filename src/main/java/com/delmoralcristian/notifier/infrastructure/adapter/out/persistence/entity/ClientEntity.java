package com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Column
    private String name;

    @Column
    private String webhookUrl;

    @Column
    private boolean active;


}
