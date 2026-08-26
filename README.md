# Notifier Event API

A Spring Boot service that ingests notification events from AWS SQS, persists them, and delivers them to client webhooks with retry logic and rate limiting.

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language |
| Spring Boot | 3.4.5 | Framework |
| Spring Cloud AWS SQS | 3.x | Message consumption |
| H2 | in-memory | Database (local/test) |
| Flyway | 10.x | Schema migrations |
| MapStruct | 1.5.5 | Entity <-> DTO mapping |
| Spring Retry | 2.x | Webhook delivery retry |
| Bucket4j | 8.10.1 | Rate limiting |
| Redis | 7 | Cache layer (Spring Cache) |
| Springdoc OpenAPI | 2.8.3 | Swagger UI |
| LocalStack | 3.0 | Local AWS emulation |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for LocalStack)

---

## Running Locally

### 1. Start LocalStack (SQS)

```bash
docker compose -f support/docker/compose.yaml up -d
```

This starts LocalStack 3.0 and Redis 7. LocalStack automatically runs `init-aws-resources.sh`, which creates:
- `event_notifications_sqs_dlq` — Dead Letter Queue (receives messages after 3 failed attempts)
- `event_notifications_sqs` — Main queue with a redrive policy pointing to the DLQ

### 2. Run the application

```bash
mvn clean install -DskipTests
mvn spring-boot:run
```

The app starts on **http://localhost:8080** with the `local` profile active.

### 3. Verify the database (optional)

- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:notifierdb`
- User: `cobre`
- Password: `cobre1234`

Flyway applies two migrations on startup:
- `V1__initialData.sql` — creates `client` and `notification_event` tables
- `V2__fillData.sql` — seeds three clients (CLIENT001, CLIENT002, CLIENT003)

---

## API Reference

All endpoints require the `X-API-Key` header.

> **Local API key:** `cobre-dev-api-key`

### Swagger UI

http://localhost:8080/swagger-ui/index.html

Click **Authorize** and enter the API key before making requests.

---

### GET /notification_events

Returns a paginated list of notification events filtered by client.

**Required:** `clientId`
**Optional:** `status`, `from`, `to` (ISO 8601 datetime), `page` (default 0), `size` (default 20)

Response is cached in Redis for 5 minutes per unique combination of parameters.

```bash
curl -X GET "http://localhost:8080/notification_events?clientId=CLIENT001&page=0&size=20" \
  -H "X-API-Key: cobre-dev-api-key"
```

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

```bash
# With filters
curl -X GET "http://localhost:8080/notification_events?clientId=CLIENT001&status=FAILED&from=2024-03-01T00:00:00&to=2024-03-31T23:59:59&page=0&size=50" \
  -H "X-API-Key: cobre-dev-api-key"
```

---

### GET /notification_events/{id}

Returns a single event by its `eventId`.

```bash
curl -X GET "http://localhost:8080/notification_events/EVT001" \
  -H "X-API-Key: cobre-dev-api-key"
```

---

### POST /notification_events/{id}/replay

Re-triggers webhook delivery for a specific event. Only events with status `FAILED` can be replayed — replaying a `COMPLETED` event returns `400 Bad Request`.

Rate limited to **10 requests/minute per API key**. On success, invalidates the Redis cache for that event.

```bash
curl -X POST "http://localhost:8080/notification_events/EVT001/replay" \
  -H "X-API-Key: cobre-dev-api-key"
```

Returns `202 Accepted`.

---

## Sending Events via SQS

```bash
# Using awslocal (alias for aws --endpoint-url http://localhost:4566)
awslocal sqs send-message \
  --queue-url http://localhost:4566/000000000000/event_notifications_sqs \
  --message-body '{
    "eventId": "EVT001",
    "eventType": "CREDIT_CARD_PAYMENT",
    "content": "Payment of $150.00",
    "deliveryDate": "2024-03-15T10:30:00",
    "clientId": "CLIENT001"
  }'
```

Or run the helper script:

```bash
bash support/docker/localstack-script.sh
```

---

## Security

| Concern | Mechanism |
|---|---|
| Authentication (OWASP A01) | `X-API-Key` header validated by `ApiKeyInterceptor` |
| Input validation (OWASP A03) | Bean Validation — `@NotBlank`, enum binding, date range check |
| Rate limiting (OWASP A05) | Bucket4j — 10 req/min per API key on the replay endpoint |

---

## Observability

Every request and SQS message is traced with MDC fields visible in all log lines:

- **`correlationId`** — from `X-Correlation-ID` header or auto-generated UUID
- **`eventId`** / **`clientId`** — set during SQS message processing

Example log line:
```
2024-03-15 10:30:00.123 [main] INFO EventConsumer [correlationId=abc123 eventId=EVT001 clientId=CLIENT001] - Processing event from SQS
```

Processing time for service methods is logged automatically via the `@TrackProcessingTime` AOP annotation.

---

## Running Tests

```bash
mvn test
```

24 tests across 4 test classes, all passing:

| Test class | Tests | Scope |
|---|---|---|
| `NotificationEventServiceTest` | 9 | Service layer (unit) |
| `DeliveryServiceTest` | 4 | Delivery idempotency + webhook |
| `DeliveryRetryHandlerTest` | 4 | Retry + recover logic |
| `NotificationEventControllerTest` | 9 | REST layer (MockMvc) |

Tests use the `test` Spring profile, which disables the SQS consumer and uses an isolated H2 database.

---

## Contact

delmoralcristian@gmail.com
