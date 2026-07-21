# JARVIS Backend Foundation

Sprint 0 keeps the backend intentionally small and modular.

## Package Map

- `com.jarvis.config`: application configuration, bean definitions, and cross-cutting setup.
- `com.jarvis.controller`: REST entry points. Empty for Sprint 0.
- `com.jarvis.dto`: request and response models used at the edges of the system.
- `com.jarvis.entity`: persistence models. Empty for Sprint 0.
- `com.jarvis.repository`: Spring Data repository contracts.
- `com.jarvis.service`: application use cases and orchestration.
- `com.jarvis.storage`: storage provider abstractions and implementations.
- `com.jarvis.telegram`: Telegram-specific adapters, isolated behind storage abstractions.
- `com.jarvis.utils`: shared helpers with no domain ownership.

## Notes

- Telegram is treated as one storage provider, not a system-wide dependency.
- PostgreSQL configuration is prepared through environment-driven properties.
- No business logic, controllers, entities, or upload flows are included in Sprint 0.
