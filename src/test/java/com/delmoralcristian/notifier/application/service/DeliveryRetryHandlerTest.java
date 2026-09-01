package com.delmoralcristian.notifier.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.delmoralcristian.notifier.exceptions.WebhookDeliveryException;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DeliveryRetryHandlerTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DeliveryRetryHandler deliveryRetryHandler;

    @Test
    void attemptDelivery_success_setsStatusCompleted() {
        var event = buildEvent();

        deliveryRetryHandler.attemptDelivery(event);

        verify(restTemplate).postForEntity(eq(event.getWebhookUrl()), eq(event.getContent()), eq(Void.class));
        assertThat(event.getDeliveryStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void attemptDelivery_restClientException_throwsWebhookDeliveryException() {
        var event = buildEvent();
        doThrow(new RestClientException("connection refused"))
            .when(restTemplate).postForEntity(
                eq(event.getWebhookUrl()),
                eq(event.getContent()),
                eq(Void.class));

        assertThatThrownBy(() -> deliveryRetryHandler.attemptDelivery(event))
            .isInstanceOf(WebhookDeliveryException.class)
            .hasMessageContaining(event.getEventId());
    }

    @Test
    void attemptDelivery_emptyWebhookUrl_throwsWebhookDeliveryException() {
        var event = buildEventWithWebhookUrl("");

        assertThatThrownBy(() -> deliveryRetryHandler.attemptDelivery(event))
            .isInstanceOf(WebhookDeliveryException.class)
            .hasMessageContaining("Invalid webhook URL");
    }

    @Test
    void recover_setsStatusFailed() {
        var event = buildEvent();
        var ex = new WebhookDeliveryException("all retries exhausted");

        deliveryRetryHandler.recover(ex, event);

        assertThat(event.getDeliveryStatus()).isEqualTo("FAILED");
    }

    private NotificationEventEntity buildEvent() {
        return buildEventWithWebhookUrl("https://webhook.example.com");
    }

    private NotificationEventEntity buildEventWithWebhookUrl(String webhookUrl) {
        return NotificationEventEntity.builder()
            .eventId("EVT001")
            .eventType("credit_card_payment")
            .content("Payment of $150.00")
            .deliveryDate(LocalDateTime.now())
            .deliveryStatus("PENDING")
            .clientId("CLIENT001")
            .webhookUrl(webhookUrl)
            .build();
    }
}
