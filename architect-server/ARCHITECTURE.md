# Architect Server - Architecture Documentation

## Overview

Architect Server follows a **Clean/Hexagonal Architecture** pattern, ensuring:
- **Independence from frameworks** - Business logic doesn't depend on Micronaut, databases, or UI
- **Testability** - Each layer can be tested independently
- **Flexibility** - Easy to swap implementations (e.g., change database, add new protocols)
- **Maintainability** - Clear separation of concerns with enforced boundaries

## Architecture Layers

```
┌──────────────────────────────────────────────────────────────┐
│                     Adapters (Outer Layer)                    │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Inbound Adapters (Drivers)                 │  │
│  │  • REST Controllers (HTTP API)                          │  │
│  │  • WebSocket Servers (Real-time events)                 │  │
│  └────────────────────────────────────────────────────────┘  │
│                              │                                │
│                              ▼                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │         Application Layer (Business Logic)              │  │
│  │                                                          │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │           Inbound Ports (Use Cases)             │   │  │
│  │  │  • ManageEngineUseCase                          │   │  │
│  │  │  • ManageProjectUseCase                         │   │  │
│  │  │  • TrackExecutionUseCase                        │   │  │
│  │  │  • TrackExecutionEventUseCase                   │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  │                       ▲                                  │  │
│  │                       │                                  │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │          Application Services                    │   │  │
│  │  │  • EngineService                                 │   │  │
│  │  │  • ProjectService                                │   │  │
│  │  │  • ExecutionService                              │   │  │
│  │  │  • ExecutionEventService                         │   │  │
│  │  │  • EventBroadcastService (Real-time)             │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  │                       │                                  │  │
│  │                       ▼                                  │  │
│  │  ┌─────────────────────────────────────────────────┐   │  │
│  │  │          Outbound Ports (Interfaces)             │   │  │
│  │  │  • EngineInstancePort                            │   │  │
│  │  │  • ProjectPort                                   │   │  │
│  │  │  • ExecutionPort                                 │   │  │
│  │  │  • ExecutionEventPort                            │   │  │
│  │  └─────────────────────────────────────────────────┘   │  │
│  │                                                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                │
│                              ▼                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │              Domain Layer (Core Business)               │  │
│  │  • EngineInstance (with business methods)              │  │
│  │  • Project                                              │  │
│  │  • Execution (with status transitions)                 │  │
│  │  • ExecutionEvent                                       │  │
│  │  • Enums: EngineStatus, ExecutionStatus                │  │
│  └────────────────────────────────────────────────────────┘  │
│                              ▲                                │
│                              │                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │           Outbound Adapters (Driven)                    │  │
│  │  • Persistence Adapters (JDBC)                          │  │
│  │  • Entity Mappers (JPA Entities)                        │  │
│  │  • Repository Implementations                           │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

## Package Structure

```
io.github.architectplatform.cloud/
├── application/                    # Core Business Logic
│   ├── domain/                     # Domain Models (Pure POJOs)
│   │   ├── EngineInstance.kt
│   │   ├── Project.kt
│   │   ├── Execution.kt
│   │   └── ExecutionEvent.kt
│   │
│   ├── ports/                      # Interfaces defining boundaries
│   │   ├── inbound/                # Use Case interfaces
│   │   │   ├── ManageEngineUseCase.kt
│   │   │   ├── ManageProjectUseCase.kt
│   │   │   ├── TrackExecutionUseCase.kt
│   │   │   └── TrackExecutionEventUseCase.kt
│   │   │
│   │   └── outbound/               # Repository interfaces
│   │       ├── EngineInstancePort.kt
│   │       ├── ProjectPort.kt
│   │       ├── ExecutionPort.kt
│   │       └── ExecutionEventPort.kt
│   │
│   └── services/                   # Application Services (Use Case implementations)
│       ├── EngineService.kt
│       ├── ProjectService.kt
│       ├── ExecutionService.kt
│       ├── ExecutionEventService.kt
│       └── EventBroadcastService.kt
│
└── adapters/                       # External interfaces
    ├── inbound/                    # Entry points (Drivers)
    │   ├── rest/                   # REST API Controllers
    │   │   ├── EngineController.kt
    │   │   ├── ProjectController.kt
    │   │   ├── ExecutionController.kt
    │   │   └── dto/                # Request/Response DTOs
    │   │       └── CloudDTO.kt
    │   │
    │   └── websocket/              # WebSocket servers
    │       └── EventsWebSocketServer.kt
    │
    └── outbound/                   # Exit points (Driven)
        └── persistence/            # Database adapters
            ├── EngineInstancePersistenceAdapter.kt
            ├── ProjectPersistenceAdapter.kt
            ├── ExecutionPersistenceAdapter.kt
            ├── ExecutionEventPersistenceAdapter.kt
            ├── entities/           # JPA Entities
            │   ├── EngineInstanceEntity.kt
            │   ├── ProjectEntity.kt
            │   ├── ExecutionEntity.kt
            │   └── ExecutionEventEntity.kt
            └── repositories/       # Micronaut Data repositories
                ├── JdbcEngineInstanceRepository.kt
                ├── JdbcProjectRepository.kt
                ├── JdbcExecutionRepository.kt
                └── JdbcExecutionEventRepository.kt
```

## Key Design Principles

### 1. Dependency Rule
**Dependencies point inward**: Outer layers depend on inner layers, never the reverse.

```
Adapters → Application → Domain
   ↓           ↓           ↓
REST/WS → Services → Models (NO outward dependencies)
```

### 2. Pure Domain Models
Domain objects have:
- **No framework annotations** (no `@Entity`, `@MappedEntity`)
- **Business logic only** (methods like `updateHeartbeat()`, `complete()`, `fail()`)
- **Immutability** (data classes with `copy()`)

Example:
```kotlin
data class Execution(
    val id: String,
    val status: ExecutionStatus,
    // ... other fields
) {
    fun complete(message: String? = null): Execution {
        return copy(
            status = ExecutionStatus.COMPLETED,
            message = message,
            completedAt = Instant.now()
        )
    }
}
```

### 3. Ports (Interfaces) Define Boundaries

**Inbound Ports (Use Cases)**: What the application can do
```kotlin
interface ManageEngineUseCase {
    fun registerEngine(...): EngineInstance
    fun recordHeartbeat(engineId: String)
    fun getEngine(engineId: String): EngineInstance?
}
```

**Outbound Ports**: What the application needs
```kotlin
interface EngineInstancePort {
    fun save(engine: EngineInstance): EngineInstance
    fun findById(id: String): EngineInstance?
    fun findAll(): List<EngineInstance>
}
```

### 4. Adapters Implement Ports

**Inbound Adapters** (REST, WebSocket) call use cases:
```kotlin
@Controller("/api/engines")
class EngineController(
    private val manageEngineUseCase: ManageEngineUseCase  // Depends on interface
) {
    @Post
    fun registerEngine(@Body request: RegisterEngineRequest) {
        return manageEngineUseCase.registerEngine(...)
    }
}
```

**Outbound Adapters** implement persistence:
```kotlin
@Singleton
class EngineInstancePersistenceAdapter(
    private val repository: JdbcEngineInstanceRepository
) : EngineInstancePort {  // Implements the port
    override fun save(engine: EngineInstance): EngineInstance {
        val entity = EngineInstanceEntity.fromDomain(engine)
        val saved = repository.save(entity)
        return saved.toDomain()
    }
}
```

## Real-Time Event Streaming

### Event Broadcasting Architecture

```
┌──────────────────┐
│  REST/WS Request │
└────────┬─────────┘
         │
         ▼
┌─────────────────────┐
│  Application Service │  ──────┐
└─────────────────────┘         │ Broadcast
         │                      │
         ▼                      ▼
┌─────────────────────┐  ┌──────────────────────┐
│ Persistence Adapter  │  │ EventBroadcastService│
└─────────────────────┘  └──────────┬───────────┘
                                    │ Reactor Sink
                                    │
                         ┌──────────▼──────────┐
                         │    Flux<CloudEvent> │
                         └──────────┬──────────┘
                                    │ Subscribe
                    ┌───────────────┼───────────────┐
                    │               │               │
              ┌─────▼─────┐   ┌────▼────┐    ┌────▼────┐
              │ WebSocket │   │WebSocket│    │WebSocket│
              │  Client 1 │   │Client 2 │    │Client 3 │
              └───────────┘   └─────────┘    └─────────┘
```

### Event Types

All domain operations broadcast events:
- `ENGINE_REGISTERED` - New engine connected
- `ENGINE_HEARTBEAT` - Engine sent heartbeat
- `PROJECT_REGISTERED` - Project registered
- `EXECUTION_STARTED` - Execution began
- `EXECUTION_COMPLETED` - Execution finished successfully
- `EXECUTION_FAILED` - Execution failed
- `EXECUTION_EVENT` - Task output/event

### WebSocket Connection

Clients connect to `/ws/events` and receive:
```json
{
  "type": "EXECUTION_COMPLETED",
  "entityId": "exec-123",
  "entityType": "EXECUTION",
  "data": {
    "id": "exec-123",
    "projectId": "proj-456",
    "status": "COMPLETED",
    "message": "Build successful"
  },
  "timestamp": "2025-11-02T12:00:00Z"
}
```

## Architecture Enforcement

### ArchUnit Tests

We use ArchUnit to automatically enforce architecture rules:

```kotlin
@Test
fun `domain layer should not depend on any other layer`() {
    noClasses()
        .that().resideInAPackage("..application.domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..application.ports..", "..adapters..")
        .check(classes)
}
```

Rules enforced:
1. ✅ Domain has no dependencies on other layers
2. ✅ Ports don't depend on adapters
3. ✅ Services only depend on domain and ports
4. ✅ Adapters implement ports
5. ✅ Use cases are interfaces
6. ✅ Domain objects have no framework annotations
7. ✅ Layered architecture respected

Run tests: `./gradlew test`

## Benefits

### 1. **Testability**
Each layer can be tested independently:
- Domain logic: Pure unit tests
- Services: Mock ports
- Adapters: Integration tests

### 2. **Flexibility**
Easy to change implementations:
- Swap H2 → PostgreSQL: Change adapter only
- Add gRPC API: New inbound adapter
- Add caching: Wrap port implementation

### 3. **Maintainability**
Clear boundaries prevent:
- Coupling between layers
- Framework lock-in
- Business logic leaking to adapters

### 4. **Scalability**
- Services can be async
- Multiple adapters can coexist
- WebSocket scales to many clients

## Technology Stack

- **Framework**: Micronaut 4.6 (lightweight, fast startup)
- **Language**: Kotlin 1.9.25 (concise, null-safe)
- **Persistence**: Micronaut Data JDBC (compile-time repositories)
- **Database**: H2 (dev), PostgreSQL/MySQL (prod)
- **Reactive**: Project Reactor (non-blocking streams)
- **WebSocket**: Micronaut WebSocket
- **Testing**: JUnit 5, ArchUnit
- **Architecture**: Clean/Hexagonal

## Further Reading

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) - Uncle Bob
- [Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/) - Alistair Cockburn
- [ArchUnit](https://www.archunit.org/) - Architecture testing
- [Micronaut Framework](https://micronaut.io/) - Framework docs
