# Database — ItemLog

`db/migrations/V1__initial.sql` creates `item_events`, `restorations`, `schema_migrations`.

- `item_events` — who, where, what, when, type
- `restorations` — audit of restores

`DataSourceProvider` gives Hikari DataSource. `RetentionService` can prune old rows. Use sqlite locally, mysql for production.
