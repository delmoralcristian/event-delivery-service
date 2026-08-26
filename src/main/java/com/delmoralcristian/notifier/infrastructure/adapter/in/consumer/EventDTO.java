package com.delmoralcristian.notifier.infrastructure.adapter.in.consumer;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {

    @NotBlank
    @JsonAlias("event_id")
    private String eventId;

    @NotBlank
    @JsonAlias("client_id")
    private String clientId;

    @NotBlank
    @JsonAlias("event_type")
    private String eventType;

    @NotBlank
    private String content;

}
