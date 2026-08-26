# Diagrams — Notifier Event API

---

## 1. System Overview

```
                        ┌────────────────────────────────────────────────────────────┐
                        │                     Notifier Service                       │
                        │                                                            │
  ┌──────────┐  JSON    │  ┌────────────────┐    ┌──────────────────────────────┐   │
  │   SQS    │─────────▶│  │  EventConsumer │───▶│       DeliveryService        │   │
  │(LocalSt.)│          │  │ (in/consumer)  │    │   (application/service)      │   │
  └──────────┘          │  └────────────────┘    └──────────────┬───────────────┘   │
                        │                                       │                   │
  ┌──────────┐  HTTP    │  ┌─────────────────────────────┐      │   save/lookup     │
  │  Client  │─────────▶│  │  NotificationEventCtrl      │      ▼                   │
  │ (Swagger │          │  │    ApiKeyInterceptor         │  ┌──────────────────┐   │
  │  / curl) │          │  │   RateLimitInterceptor       │  │   H2 In-Memory   │   │
  └──────────┘          │  │   CorrelationIdFilter        │  │    Database      │   │
                        │  │   GlobalExceptionHandler     │  │  (Flyway V1+V2)  │   │
                        │  └──────────────┬───────────────┘  └──────────────────┘   │
                        │                 │                                          │
                        │                 ▼                  ┌──────────────────┐   │
                        │  ┌──────────────────────────────┐  │      Redis       │   │
                        │  │   NotificationEventService   │◀▶│  (Cache 5 min)   │   │
                        │  │  @Cacheable / @CacheEvict    │  │   port 6379      │   │
                        │  └──────────────────────────────┘  └──────────────────┘   │
                        │                 │                                          │
                        │                 └──────── webhook POST ──▶ Client Webhook  │
                        └────────────────────────────────────────────────────────────┘
```

---

## 2. Package Structure (Hexagonal Architecture)

```
com.delmoralcristian.notifier
│
├── application/
│   ├── dto/                       # Data transfer objects
│   ├── port/
│   │   ├── in/                    # Use-case interfaces (input ports)
│   │   └── out/                   # Repository interfaces (output ports)
│   └── service/                   # Business logic
│       ├── NotificationEventService
│       ├── DeliveryService
│       └── DeliveryRetryHandler   # @Retryable + @Recover
│
├── infrastructure/
│   └── adapter/
│       ├── in/
│       │   ├── consumer/          # SQS listener (EventConsumer)
│       │   └── web/               # REST controller + security filters
│       └── out/
│           ├── mapper/            # MapStruct entity <-> DTO
│           └── persistence/       # JPA entities, repositories, adapters
│
├── config/                        # Spring configuration beans
├── advice/                        # AOP: @RestControllerAdvice, @TrackProcessingTime
├── enums/                         # EEventType, ENotificationStatus
└── exceptions/                    # WebhookDeliveryException
```

---

## 3. Architecture Diagram

Arquitectura hexagonal (Ports & Adapters) con dos canales de entrada: SQS y REST.

```mermaid
flowchart TB
    subgraph EXTERNAL["Sistemas Externos"]
        SQS["AWS SQS\nevent_notifications_sqs"]
        DLQ["AWS SQS DLQ\nevent_notifications_sqs_dlq"]
        CLIENT["Cliente HTTP\n(Swagger / curl)"]
        WEBHOOK["Webhook del Cliente\n(HTTP endpoint)"]
    end

    subgraph APP["Notifier Service (Spring Boot)"]
        subgraph IN_ADAPTERS["Adaptadores de Entrada"]
            EC["EventConsumer\n@SqsListener"]
            CTRL["NotificationEventController\nGET /notification_events\nPOST /notification_events/{id}/replay"]
            CORR["CorrelationIdFilter\n(MDC / correlationId)"]
            AKEY["ApiKeyInterceptor\n(OWASP A01)"]
            RLIM["RateLimitInterceptor\nBucket4j (OWASP A05)"]
        end

        subgraph CORE["Núcleo de Negocio (Application)"]
            SVC["NotificationEventService\n(puerto: NotificationEventUseCase)"]
            DS["DeliveryService\n(puerto: DeliveryServiceUseCase)"]
            DRH["DeliveryRetryHandler\n@Retryable + @Recover"]
        end

        subgraph OUT_ADAPTERS["Adaptadores de Salida"]
            NPA["NotificationEventPersistenceAdapter\n(puerto: NotificationEventPersistencePort)"]
            CPA["ClientPersistenceAdapter\n(puerto: ClientPersistencePort)"]
        end

        subgraph DB["Persistencia"]
            H2["H2 In-Memory DB\nFlyway V1 + V2"]
        end

        REDIS["Redis\n@Cacheable events\n@Cacheable event-pages\nTTL: 5 min"]
    end

    SQS -->|"mensaje JSON"| EC
    EC -->|"no ack → reintento"| SQS
    EC -->|"3 fallos SQS"| DLQ
    CLIENT -->|"HTTP + X-API-Key"| CORR
    CORR --> AKEY
    AKEY --> RLIM
    RLIM --> CTRL

    EC --> DS
    CTRL --> SVC
    SVC <-->|"cache hit/miss\n@CacheEvict on replay"| REDIS
    SVC --> DS
    SVC --> NPA
    SVC --> CPA
    DS --> DRH
    DS --> NPA
    DS --> CPA
    DRH -->|"POST webhook"| WEBHOOK

    NPA --> H2
    CPA --> H2
```

---

## 4. Class Diagram

Relaciones entre los componentes principales siguiendo el patrón Puertos y Adaptadores.

```mermaid
classDiagram
    direction TB

    class NotificationEventUseCase {
        <<interface>>
        +findByFilters(clientId, status, from, to, page, size) PagedResponse~NotificationEventDTO~
        +getByEventId(eventId) NotificationEventDTO
        +replayNotification(eventId) void
    }

    class DeliveryServiceUseCase {
        <<interface>>
        +send(event EventDTO) void
        +reSend(event NotificationEventEntity) void
    }

    class NotificationEventPersistencePort {
        <<interface>>
        +findByEventId(eventId) Optional~NotificationEventEntity~
        +existsByEventIdAndClientId(eventId, clientId) boolean
        +findByFilters(clientId, status, from, to, pageable) Page
        +save(entity) NotificationEventEntity
    }

    class ClientPersistencePort {
        <<interface>>
        +findById(id) Optional~ClientEntity~
    }

    class NotificationEventService {
        -notificationAdapter NotificationEventPersistencePort
        -clientAdapter ClientPersistencePort
        -deliveryService DeliveryService
        +findByFilters(clientId, status, from, to, page, size) @Cacheable
        +getByEventId(eventId) @Cacheable
        +replayNotification(eventId) @CacheEvict
    }

    class DeliveryService {
        -notificationAdapter NotificationEventPersistencePort
        -clientAdapter ClientPersistencePort
        -retryHandler DeliveryRetryHandler
        +send(eventDTO)
        +reSend(entity)
    }

    class DeliveryRetryHandler {
        -restTemplate RestTemplate
        +attemptDelivery(entity) void
        +recover(ex, entity) void
    }

    class NotificationEventController {
        -service NotificationEventService
        +getAll(clientId, status, from, to, page, size) 200
        +getByEventId(id) 200
        +replay(id) 202
    }

    class EventConsumer {
        -deliveryService DeliveryService
        +processMessage(message, headers, ack)
    }

    class NotificationEventPersistenceAdapter {
        -jpaRepository JpaNotificationEventRepository
        +findByEventId(eventId)
        +existsByEventIdAndClientId(eventId, clientId)
        +findByFilters(...)
        +save(entity)
    }

    class ClientPersistenceAdapter {
        -jpaRepository JpaClientRepository
        +findById(id)
    }

    class NotificationEventEntity {
        +Long id
        +String eventId
        +String eventType
        +String content
        +LocalDateTime deliveryDate
        +String deliveryStatus
        +ClientEntity client
    }

    class ClientEntity {
        +String id
        +String name
        +String webhookUrl
        +Boolean active
    }

    NotificationEventService ..|> NotificationEventUseCase : implements
    DeliveryService ..|> DeliveryServiceUseCase : implements
    NotificationEventPersistenceAdapter ..|> NotificationEventPersistencePort : implements
    ClientPersistenceAdapter ..|> ClientPersistencePort : implements

    NotificationEventService --> NotificationEventPersistencePort : usa
    NotificationEventService --> ClientPersistencePort : usa
    NotificationEventService --> DeliveryService : delega

    DeliveryService --> NotificationEventPersistencePort : usa
    DeliveryService --> ClientPersistencePort : usa
    DeliveryService --> DeliveryRetryHandler : delega

    NotificationEventController --> NotificationEventService : usa
    EventConsumer --> DeliveryService : usa

    NotificationEventEntity --> ClientEntity : ManyToOne
```

---

## 5. Sequence Diagram — SQS Event Ingestion

Flujo completo desde que SQS entrega el mensaje hasta la entrega al webhook del cliente, incluyendo retry y DLQ.

```mermaid
sequenceDiagram
    actor SQS as AWS SQS
    participant EC as EventConsumer
    participant DS as DeliveryService
    participant DB as H2 Database
    participant DRH as DeliveryRetryHandler
    participant WH as Client Webhook

    SQS->>EC: processMessage(JSON, headers, ack)
    EC->>EC: parse → EventDTO
    EC->>EC: MDC.put(eventId, clientId)

    EC->>DS: send(eventDTO)
    DS->>DB: findById(clientId)
    DB-->>DS: ClientEntity

    DS->>DB: existsByEventIdAndClientId(eventId, clientId)
    DB-->>DS: false

    DS->>DB: save(entity, status=PENDING)
    DB-->>DS: saved entity

    DS->>DRH: attemptDelivery(entity)

    alt Entrega exitosa
        DRH->>WH: POST webhookUrl (content)
        WH-->>DRH: 2xx OK
        DRH->>DRH: entity.status = COMPLETED
        DRH->>DB: save(entity, status=COMPLETED)
        EC->>SQS: ack.acknowledge()
    else Webhook falla — Spring Retry activo
        loop Hasta 4 intentos (2s entre cada uno)
            DRH->>WH: POST webhookUrl
            WH-->>DRH: 5xx / timeout
            DRH->>DRH: lanza WebhookDeliveryException
        end
        DRH->>DRH: @Recover → entity.status = FAILED
        DRH->>DB: save(entity, status=FAILED)
        EC->>SQS: ack.acknowledge()
    else Error de parseo / excepción inesperada
        EC->>EC: catch Exception → log.error(...)
        Note over EC,SQS: Sin ack → SQS reintenta el mensaje
        Note over EC,SQS: Después de 3 intentos SQS → mueve a DLQ
    end

    EC->>EC: MDC.remove(eventId, clientId)
```

---

## 6. Sequence Diagram — GET /notification_events

Consulta paginada con autenticación, validación de inputs y cache Redis.

```mermaid
sequenceDiagram
    actor C as Cliente HTTP
    participant CF as CorrelationIdFilter
    participant AKI as ApiKeyInterceptor
    participant CTRL as NotificationEventController
    participant SVC as NotificationEventService
    participant REDIS as Redis Cache
    participant DB as H2 Database

    C->>CF: GET /notification_events?clientId=CLIENT001&page=0&size=20
    CF->>CF: leer X-Correlation-ID o generar UUID
    CF->>CF: MDC.put("correlationId", id)

    CF->>AKI: forward request

    alt API key ausente o inválida
        AKI-->>C: 401 Unauthorized
    else API key válida
        AKI->>CTRL: getAll(clientId, status, from, to, page, size)

        alt clientId vacío o ausente
            CTRL-->>C: 400 Bad Request
        else Rango de fechas inválido (from > to)
            CTRL-->>C: 400 Bad Request
        else Inputs válidos
            CTRL->>SVC: findByFilters(clientId, ..., page, size)
            SVC->>REDIS: get("event-pages::CLIENT001:null:null:null:0:20")

            alt Cache hit
                REDIS-->>SVC: PagedResponse (cached)
            else Cache miss
                SVC->>DB: findById(clientId)

                alt Cliente no existe
                    SVC-->>C: 404 Not Found
                else Cliente existe
                    SVC->>DB: findByFilters(clientId, status, from, to, pageable)
                    DB-->>SVC: Page~NotificationEventEntity~
                    SVC->>SVC: mapper → PagedResponse~NotificationEventDTO~
                    SVC->>REDIS: put("event-pages::...", PagedResponse, TTL=5min)
                end
            end

            SVC-->>CTRL: PagedResponse~NotificationEventDTO~
            CTRL-->>C: 200 OK {content, page, size, totalElements, totalPages}
        end
    end
```

---

## 7. Sequence Diagram — POST /notification_events/{id}/replay

Reintento manual de entrega con rate limiting, validación de estado y evicción de cache.

```mermaid
sequenceDiagram
    actor C as Cliente HTTP
    participant AKI as ApiKeyInterceptor
    participant RLI as RateLimitInterceptor
    participant CTRL as NotificationEventController
    participant SVC as NotificationEventService
    participant REDIS as Redis Cache
    participant DS as DeliveryService
    participant DRH as DeliveryRetryHandler
    participant WH as Client Webhook

    C->>AKI: POST /notification_events/EVT001/replay

    alt API key inválida
        AKI-->>C: 401 Unauthorized
    else API key válida
        AKI->>RLI: forward

        alt Límite excedido (> 10 req/min para esta key)
            RLI-->>C: 429 Too Many Requests
        else Dentro del límite
            RLI->>CTRL: replay("EVT001")
            CTRL->>SVC: replayNotification("EVT001")
            SVC->>DB: findByEventId("EVT001")

            alt Evento no encontrado
                SVC-->>C: 404 Not Found
            else Estado = COMPLETED
                SVC-->>C: 400 Bad Request {"error": "Event EVT001 is already COMPLETED"}
            else Estado = FAILED o PENDING
                SVC->>DS: reSend(entity)
                DS->>DRH: attemptDelivery(entity)
                Note over DRH,WH: Mismo flujo de retry que en SQS
                DRH->>WH: POST webhookUrl
                WH-->>DRH: respuesta
                SVC->>REDIS: evict("events::EVT001")
                SVC->>REDIS: evict all("event-pages::*")
                CTRL-->>C: 202 Accepted
            end
        end
    end
```
