#!/bin/bash
# Manual SQS helper commands for local development
# Run these against LocalStack at http://localhost:4566

# List all queues
aws --endpoint-url=http://localhost:4566 sqs list-queues

# Send a test event to the main queue
aws --endpoint-url=http://localhost:4566 sqs send-message \
  --region=eu-west-1 \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs \
  --message-body '{"event_id":"EVT014","client_id":"CLIENT001","event_type":"credit_card_payment","content":"Credit card payment received for $150.00"}'

# Check messages in main queue
aws --endpoint-url=http://localhost:4566 sqs receive-message \
  --region=eu-west-1 \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs

# Check messages in DLQ
aws --endpoint-url=http://localhost:4566 sqs receive-message \
  --region=eu-west-1 \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs_dlq

# Purge main queue
aws --endpoint-url=http://localhost:4566 sqs purge-queue \
  --region=eu-west-1 \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs
