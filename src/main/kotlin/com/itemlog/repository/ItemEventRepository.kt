package com.itemlog.repository

import com.itemlog.model.EventType
import com.itemlog.model.ItemEvent
import com.itemlog.model.ItemSnapshot
import com.itemlog.model.LocationData
import java.nio.ByteBuffer
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class ItemEventRepository(private val ds: DataSource) {

    fun insert(event: ItemEvent) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                INSERT INTO item_events 
                (event_id, event_type, timestamp, player_uuid, world, x, y, z, yaw, pitch, material, before_json, after_json, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setBytes(1, uuidToBytes(event.eventId))
                ps.setString(2, event.type.name)
                ps.setLong(3, event.timestamp)
                if (event.playerId != null) ps.setBytes(4, uuidToBytes(event.playerId)) else ps.setNull(4, java.sql.Types.BINARY)
                ps.setString(5, event.location?.world)
                if (event.location != null) {
                    ps.setDouble(6, event.location.x)
                    ps.setDouble(7, event.location.y)
                    ps.setDouble(8, event.location.z)
                    ps.setFloat(9, event.location.yaw)
                    ps.setFloat(10, event.location.pitch)
                } else {
                    ps.setNull(6, java.sql.Types.DOUBLE)
                    ps.setNull(7, java.sql.Types.DOUBLE)
                    ps.setNull(8, java.sql.Types.DOUBLE)
                    ps.setNull(9, java.sql.Types.FLOAT)
                    ps.setNull(10, java.sql.Types.FLOAT)
                }
                ps.setString(11, event.material)
                ps.setString(12, event.before?.let { snapshotToJson(it) })
                ps.setString(13, event.after?.let { snapshotToJson(it) })
                ps.setString(14, event.source)
                ps.executeUpdate()
            }
        }
    }

    fun insertBatch(events: List<ItemEvent>) {
        if (events.isEmpty()) return
        ds.connection.use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement(
                    """
                    INSERT INTO item_events 
                    (event_id, event_type, timestamp, player_uuid, world, x, y, z, yaw, pitch, material, before_json, after_json, source)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    for (event in events) {
                        ps.setBytes(1, uuidToBytes(event.eventId))
                        ps.setString(2, event.type.name)
                        ps.setLong(3, event.timestamp)
                        if (event.playerId != null) ps.setBytes(4, uuidToBytes(event.playerId)) else ps.setNull(4, java.sql.Types.BINARY)
                        ps.setString(5, event.location?.world)
                        if (event.location != null) {
                            ps.setDouble(6, event.location.x)
                            ps.setDouble(7, event.location.y)
                            ps.setDouble(8, event.location.z)
                            ps.setFloat(9, event.location.yaw)
                            ps.setFloat(10, event.location.pitch)
                        } else {
                            ps.setNull(6, java.sql.Types.DOUBLE)
                            ps.setNull(7, java.sql.Types.DOUBLE)
                            ps.setNull(8, java.sql.Types.DOUBLE)
                            ps.setNull(9, java.sql.Types.FLOAT)
                            ps.setNull(10, java.sql.Types.FLOAT)
                        }
                        ps.setString(11, event.material)
                        ps.setString(12, event.before?.let { snapshotToJson(it) })
                        ps.setString(13, event.after?.let { snapshotToJson(it) })
                        ps.setString(14, event.source)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }

    fun findById(eventId: UUID): ItemEvent? {
        ds.connection.use { c ->
            c.prepareStatement("SELECT * FROM item_events WHERE event_id = ?").use { ps ->
                ps.setBytes(1, uuidToBytes(eventId))
                ps.executeQuery().use { rs ->
                    if (rs.next()) return mapRow(rs)
                }
            }
        }
        return null
    }

    data class Filter(
        val playerId: UUID? = null,
        val eventType: EventType? = null,
        val fromTime: Long? = null,
        val toTime: Long? = null,
        val material: String? = null
    )

    data class Pagination(
        val limit: Int = 50,
        val offset: Int = 0
    )

    fun find(filter: Filter, pagination: Pagination): List<ItemEvent> {
        val sql = StringBuilder("SELECT * FROM item_events WHERE 1=1")
        val params = mutableListOf<Any?>()

        if (filter.playerId != null) {
            sql.append(" AND player_uuid = ?")
            params.add(uuidToBytes(filter.playerId))
        }
        if (filter.eventType != null) {
            sql.append(" AND event_type = ?")
            params.add(filter.eventType.name)
        }
        if (filter.fromTime != null) {
            sql.append(" AND timestamp >= ?")
            params.add(filter.fromTime)
        }
        if (filter.toTime != null) {
            sql.append(" AND timestamp <= ?")
            params.add(filter.toTime)
        }
        if (filter.material != null) {
            sql.append(" AND material = ?")
            params.add(filter.material)
        }
        sql.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?")
        params.add(pagination.limit)
        params.add(pagination.offset)

        ds.connection.use { c ->
            c.prepareStatement(sql.toString()).use { ps ->
                for ((i, p) in params.withIndex()) {
                    when (p) {
                        is ByteArray -> ps.setBytes(i + 1, p)
                        is String -> ps.setString(i + 1, p)
                        is Long -> ps.setLong(i + 1, p)
                        is Int -> ps.setInt(i + 1, p)
                    }
                }
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<ItemEvent>()
                    while (rs.next()) list.add(mapRow(rs))
                    return list
                }
            }
        }
    }

    private fun mapRow(rs: java.sql.ResultSet): ItemEvent {
        val eventId = bytesToUuid(rs.getBytes("event_id"))
        val type = EventType.valueOf(rs.getString("event_type"))
        val timestamp = rs.getLong("timestamp")
        val playerBytes = rs.getBytes("player_uuid")
        val playerId = if (playerBytes != null) bytesToUuid(playerBytes) else null
        val world = rs.getString("world")
        val x = rs.getDouble("x")
        val y = rs.getDouble("y")
        val z = rs.getDouble("z")
        val yaw = rs.getFloat("yaw")
        val pitch = rs.getFloat("pitch")
        val location = if (world != null && !rs.wasNull()) LocationData(world, x, y, z, yaw, pitch) else null
        val beforeJson = rs.getString("before_json")
        val afterJson = rs.getString("after_json")
        val before = beforeJson?.let { jsonToSnapshot(it) }
        val after = afterJson?.let { jsonToSnapshot(it) }
        val material = rs.getString("material")
        val source = rs.getString("source")
        return ItemEvent(eventId, type, timestamp, playerId, location, before, after, source)
    }

    private fun snapshotToJson(s: ItemSnapshot): String {
        // Simple JSON for snapshot: material|amount|itemJson
        return "${s.material}|${s.amount}|${s.itemJson ?: ""}"
    }

    private fun jsonToSnapshot(s: String): ItemSnapshot {
        val parts = s.split("|", limit = 3)
        return ItemSnapshot(parts[0], parts[1].toInt(), parts.getOrNull(2)?.ifEmpty { null })
    }

    private fun uuidToBytes(uuid: UUID): ByteArray {
        val bb = ByteBuffer.wrap(ByteArray(16))
        bb.putLong(uuid.mostSignificantBits)
        bb.putLong(uuid.leastSignificantBits)
        return bb.array()
    }

    private fun bytesToUuid(bytes: ByteArray): UUID {
        val bb = ByteBuffer.wrap(bytes)
        return UUID(bb.long, bb.long)
    }
}
