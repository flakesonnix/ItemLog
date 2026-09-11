# Architecture — ItemLog

Backend only — no commands. Logs every item move to DB for ItemLogAdmin.

```
Paper
  └─ ItemLogPlugin
       ├─ DataSourceProvider — HikariCP (sqlite/mysql)
       ├─ MigrationRunner — V1__initial.sql → item_events, restorations
       ├─ Listeners — Pickup, Drop, Death, Container, CraftSmelt, Inventory, ConsumeDestroy, BlockItem
       ├─ EventBuffer + Deduplicator — ordered, crash-safe
       └─ Repositories — ItemEventRepository, RestorationRepository
```

Each listener calls `ItemLogService.log(event)` → buffered → batch insert. Deduplicator avoids double-log from Bukkit.
