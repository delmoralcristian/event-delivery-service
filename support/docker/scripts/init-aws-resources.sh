#!/bin/bash

# Create DLQ first
aws sqs create-queue --queue-name event_notifications_sqs_dlq

DLQ_ARN=$(aws sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs_dlq \
  --attribute-names QueueArn \
  --query Attributes.QueueArn \
  --output text)

echo "DLQ ARN: $DLQ_ARN"

# Create main queue with redrive policy pointing to DLQ
aws sqs create-queue \
  --queue-name event_notifications_sqs \
  --attributes "DelaySeconds=10,VisibilityTimeout=60,RedrivePolicy={\"deadLetterTargetArn\":\"$DLQ_ARN\",\"maxReceiveCount\":\"3\"}"

aws sqs list-queues
