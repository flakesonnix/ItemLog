# Configuration — ItemLog

`plugins/ItemLog/config.yml`:

```yaml
database:
  type: sqlite
  sqlite: { file: database.db }
  mysql: { host: localhost, database: itemlog, user: root, password: "" }
```

Only DB config. No commands. Keep DB file same as ItemLogAdmin for shared access.
