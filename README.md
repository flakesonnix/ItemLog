# ItemLog

Paper 1.26.2 — Kotlin — logs item events for restore.

Tracks: pickup, drop, inventory, container, death, craft, smelt, consume, destroy.

Tables: `item_events`, `restorations`, `schema_migrations`.

```bash
gradle shadowJar
# → build/libs/itemlog-1.0.0-SNAPSHOT.jar
```

Config: `plugins/ItemLog/database.db` (sqlite) or MySQL.

Backend for ItemLogAdmin.
