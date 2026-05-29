# IM Ticket System (IM工单系统)

A multi-channel customer service ticketing platform integrating WeCom (企业微信) and Feishu (飞书) IM channels with AI-powered smart routing and knowledge base retrieval.

## Architecture

```
┌──────────────────────────────────────────────┐
│  API Layer (im-ticket-api/)                  │
│  REST Controller + WebSocket                 │
├──────────────────────────────────────────────┤
│  Application Services (service/application/) │
│  Ticket / Routing / Knowledge / Duty         │
├──────────────────────────────────────────────┤
│  Domain Services (service/domain/)           │
│  State Machine / Rule Engine / Duty Assign   │
├──────────────────────────────────────────────┤
│  Infrastructure (im-ticket-infra/)           │
│  Channel Adapters / AI / MQ / Cache          │
├──────────────────────────────────────────────┤
│  Data Access (im-ticket-dao/)                │
│  MyBatis-Plus Mapper + Entity                │
├──────────────────────────────────────────────┤
│  Common (im-ticket-common/)                  │
│  DTO / Enum / Exception / Constants          │
└──────────────────────────────────────────────┘
```

**Frontend** (`im-ticket-UI/`): Vue 3 + TypeScript + Element Plus + Pinia + STOMP over WebSocket

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.3, MyBatis-Plus 3.5, Spring AI 1.0 |
| Database | MySQL 8.0 |
| Cache | Redis 7 + Redisson 3.32 |
| Message Queue | RabbitMQ 3.13 |
| Search | Elasticsearch 8.x |
| State Machine | Spring StateMachine 4.0 |
| Frontend | Vue 3.4, Element Plus 2.8, Pinia 2.2, Vite 5 |
| Real-time | STOMP over WebSocket (SockJS) |

## Key Features

- **Smart Routing**: Rule engine (fast path) → LLM intent recognition (fallback) → default ticket creation (degraded)
- **Ticket State Machine**: 8 states — PENDING → IN_PROGRESS → RESOLVED → WAITING_CONFIRM → CLOSED (with TRANSFERRED, REJECTED, DEFERRED branches)
- **Knowledge Base**: Three-tier retrieval — FAQ (MySQL exact match) → Document RAG (ES vector) → Historical tickets (ES vector)
- **Duty Assignment**: 5-level fallback — context inheritance → primary duty → backup duty → unassigned pool → alert
- **Channel Abstraction**: Adapter pattern — unified internal message format, add new channels without modifying business logic
- **Notifications**: Dual-channel — internal (WebSocket push + IM Bot) and external (reply to source IM channel)

## Project Structure

```
imdesk/
├── im-ticket-common/        # Shared: DTOs, enums, exceptions
├── im-ticket-dao/           # MyBatis-Plus entities & mappers
├── im-ticket-infra/         # AI client, channel adapters, cache, MQ, notifications
├── im-ticket-service/       # Domain + application services
├── im-ticket-api/           # REST controllers, WebSocket, exception handler
├── im-ticket-UI/            # Vue 3 frontend SPA
├── docker-compose.yml       # MySQL + Redis + RabbitMQ test environment
└── docs/                    # Design spec & development plan
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Node.js 18+ (for frontend)

### 1. Start Infrastructure

```bash
docker compose up -d
```

This starts MySQL 8.0 (port 3306), Redis 7 (port 6379), and RabbitMQ 3.13 (ports 5672/15672).

### 2. Build & Run Backend

```bash
mvn clean package -DskipTests
java -jar im-ticket-api/target/im-ticket-api-1.0.0-SNAPSHOT.jar
```

The API server starts at `http://localhost:8082`.

### 3. Run Frontend (Development)

```bash
cd im-ticket-UI
npm install
npm run dev
```

Dev server starts at `http://localhost:5173` with API proxy to `localhost:8082`.

### 4. Run Tests

```bash
# Unit tests (no Docker required)
mvn test -pl im-ticket-service

# E2E tests (requires Docker Compose running)
mvn test -pl im-ticket-api -Dspring.profiles.active=test
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/tickets` | List tickets (paginated) |
| GET | `/api/v1/tickets/{id}` | Ticket detail with messages |
| POST | `/api/v1/tickets/{id}/accept` | Accept ticket |
| POST | `/api/v1/tickets/{id}/reply` | Agent reply |
| POST | `/api/v1/tickets/{id}/transfer` | Transfer to another agent |
| POST | `/api/v1/tickets/{id}/reject` | Reject ticket |
| POST | `/api/v1/tickets/{id}/defer` | Defer ticket |
| POST | `/api/v1/tickets/{id}/resolve` | Mark resolved |
| POST | `/api/v1/tickets/{id}/close` | Close ticket |
| POST | `/api/v1/webhook/wecom` | WeCom callback |
| POST | `/api/v1/webhook/feishu` | Feishu callback |
| GET | `/api/v1/duty/pending-count` | Pending ticket count |
| GET | `/api/v1/agents` | List agents |
| GET | `/api/v1/capitals` | List capitals |
| GET | `/api/v1/shifts` | List shifts |
| GET | `/api/v1/knowledge/faqs` | List FAQs |

All responses follow the format: `{"code": 0, "message": "success", "data": {...}}`

## WebSocket

Connect via STOMP over SockJS at `/ws`:

| Topic | Description |
|-------|-------------|
| `/topic/agent/{agentId}/new-ticket` | New ticket assigned |
| `/topic/ticket/{ticketId}/status` | Ticket status change |
| `/topic/duty/unassigned-alert` | Unassigned pool alert |


## License

MIT
