# ItemLog

Paper 1.26.2 — Kotlin — quiet backend that logs item events for later restore.

Tracks: pickup, drop, inventory, container, death, craft, smelt, consume, destroy. Stores `item_events`, `restorations`, `schema_migrations` via HikariCP (sqlite/mysql).

```bash
gradle shadowJar
# → build/libs/itemlog-1.0.0-SNAPSHOT.jar
```

Just drop the jar in `plugins/` and restart — `plugins/ItemLog/database.db` is created automatically.

## Config

`plugins/ItemLog/config.yml`:

```yaml
database:
  type: sqlite # or mysql
  sqlite: { file: database.db }
  mysql: { host: localhost, database: itemlog }
```

Switch type → restart. No code change.

## How it works

Listeners buffer events (deduped, ordered) → `ItemEventRepository` → DB. `ItemLogAdmin` reads the same DB to offer `/itemlog` GUI — no shared code, DB is the contract.

## Dev

```bash
nix develop
gradle shadowJar
gradle test
nix fmt
```

See `docs/` for architecture and retention.
