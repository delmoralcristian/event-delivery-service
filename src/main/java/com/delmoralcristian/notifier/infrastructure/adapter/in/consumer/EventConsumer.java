package com.delmoralcristian.notifier.infrastructure.adapter.in.consumer;

import com.delmoralcristian.notifier.application.service.DeliveryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(value = "aws.sqs.consumer.event-notifications.enabled", havingValue = "true")
@RequiredArgsConstructor
public class EventConsumer {

    private static final TypeReference<EventDTO> EVENT_TYPE_REFERENCE = new TypeReference<>() {};

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @SqsListener(
        value = "${aws.sqs.consumer.event-notifications.queue-name}",
        maxConcurrentMessages = "${aws.sqs.consumer.event-notifications.maxConcurrentMessage:5}",
        maxMessagesPerPoll = "${aws.sqs.consumer.event-notifications.maxMessagesPerPoll:5}",
        pollTimeoutSeconds = "${aws.sqs.consumer.event-notifications.pollTimeoutSeconds:20}",
        acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL
    )
    public void processMessage(@Payload String message, @Headers MessageHeaders headers, Acknowledgement ack) {
        EventDTO event = null;
        try {
            event = objectMapper.readValue(message, EVENT_TYPE_REFERENCE);
            MDC.put("eventId", event.getEventId());
            MDC.put("clientId", event.getClientId());

            log.info("Processing event from SQS");
            this.deliveryService.send(event);
            ack.acknowledge();
            log.info("Event processed and acknowledged");
        } catch (Exception e) {
            log.error("Failed to process event — message will be retried by SQS. Raw: {}", message, e);
        } finally {
            MDC.remove("eventId");
            MDC.remove("clientId");
        }
    }
}
