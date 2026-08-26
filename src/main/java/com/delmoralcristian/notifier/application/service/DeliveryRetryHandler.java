package com.delmoralcristian.notifier.application.service;

import static com.delmoralcristian.notifier.enums.ENotificationStatus.COMPLETED;
import static com.delmoralcristian.notifier.enums.ENotificationStatus.FAILED;

import com.delmoralcristian.notifier.exceptions.WebhookDeliveryException;
import com.delmoralcristian.notifier.infrastructure.adapter.out.persistence.entity.NotificationEventEntity;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryRetryHandler {

    private final RestTemplate restTemplate;

    @Retryable(
        value = WebhookDeliveryException.class,
        maxAttemptsExpression = "#{@retryProperties.maxAttempts}",
        backoff = @Backoff(delayExpression = "#{@retryProperties.backoffDelay}")
    )
    public void attemptDelivery(NotificationEventEntity event) {
        var client = event.getClient();
        var webhookUrl = client.getWebhookUrl();

        if (StringUtils.isEmpty(webhookUrl)) {
            throw new WebhookDeliveryException("Invalid webhook URL for event " + event.getEventId());
        }

        try {
            log.info("Attempting to deliver event {} to webhook URL {}", event.getEventId(), webhookUrl);
            this.restTemplate.postForEntity(webhookUrl, event.getContent(), Void.class);
            event.setDeliveryStatus(COMPLETED.name());
        } catch (RestClientException ex) {
            throw new WebhookDeliveryException("Delivery failed for event " + event.getEventId(), ex);
        }
    }

    @Recover
    public void recover(WebhookDeliveryException ex, NotificationEventEntity event) {
        event.setDeliveryStatus(FAILED.name());
        log.error("Recover: Final retry failed for event {}. Marking as FAILED.", event.getEventId());
    }
}

