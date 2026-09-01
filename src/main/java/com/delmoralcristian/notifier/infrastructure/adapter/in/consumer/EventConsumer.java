package com.delmoralcristian.notifier.infrastructure.adapter.in.consumer;

import com.delmoralcristian.notifier.application.service.DeliveryService;
import com.delmoralcristian.notifier.utils.LockService;
import com.delmoralcristian.notifier.utils.MdcKeys;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.annotation.SqsListenerAcknowledgementMode;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import java.util.UUID;
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
    private static final String LOCK_TYPE = "EVENT";

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;
    private final LockService lockService;

    @SqsListener(
        value = "${aws.sqs.consumer.event-notifications.queue-name}",
        maxConcurrentMessages = "${aws.sqs.consumer.event-notifications.maxConcurrentMessage:5}",
        maxMessagesPerPoll = "${aws.sqs.consumer.event-notifications.maxMessagesPerPoll:5}",
        pollTimeoutSeconds = "${aws.sqs.consumer.event-notifications.pollTimeoutSeconds:20}",
        acknowledgementMode = SqsListenerAcknowledgementMode.MANUAL
    )
    public void processMessage(@Payload String message, @Headers MessageHeaders headers, Acknowledgement ack) {
        try {
            final var event = objectMapper.readValue(message, EVENT_TYPE_REFERENCE);
            MDC.put(MdcKeys.CORRELATION_ID, UUID.randomUUID().toString());
            MDC.put(MdcKeys.EVENT_ID, event.getEventId());
            MDC.put(MdcKeys.CLIENT_ID, event.getClientId());

            try {
                log.info("Processing event from SQS");

                var lockId = event.getEventId() + ":" + event.getClientId();
                var processed = lockService.executeWithLock(LOCK_TYPE, lockId, () -> deliveryService.send(event));

                if (processed) {
                    ack.acknowledge();
                    log.info("Event processed and acknowledged");
                } else {
                    log.warn("Could not acquire lock for event {} — will be retried by SQS", event.getEventId());
                }
            } finally {
                MDC.remove(MdcKeys.CORRELATION_ID);
                MDC.remove(MdcKeys.EVENT_ID);
                MDC.remove(MdcKeys.CLIENT_ID);
            }
        } catch (Exception e) {
            log.error("Failed to process event — message will be retried by SQS. Raw: {}", message, e);
        }
    }
}
