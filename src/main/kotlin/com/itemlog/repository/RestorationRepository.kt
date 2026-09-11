package com.itemlog.repository

import com.itemlog.model.LocationData
import com.itemlog.model.Restoration
import java.nio.ByteBuffer
import java.util.UUID
import javax.sql.DataSource

class RestorationRepository(private val ds: DataSource) {

    fun insert(r: Restoration) {
        ds.connection.use { c ->
            c.prepareStatement(
                """
                INSERT INTO restorations 
                (restoration_id, event_id, admin_uuid, target_uuid, timestamp, world, x, y, z, yaw, pitch, result_json, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setBytes(1, uuidToBytes(r.restorationId))
                ps.setBytes(2, uuidToBytes(r.eventId))
                ps.setBytes(3, uuidToBytes(r.adminId))
                ps.setBytes(4, uuidToBytes(r.targetId))
                ps.setLong(5, r.timestamp)
                ps.setString(6, r.restoreLocation?.world)
                if (r.restoreLocation != null) {
                    ps.setDouble(7, r.restoreLocation.x)
                    ps.setDouble(8, r.restoreLocation.y)
                    ps.setDouble(9, r.restoreLocation.z)
                    ps.setFloat(10, r.restoreLocation.yaw)
                    ps.setFloat(11, r.restoreLocation.pitch)
                } else {
                    ps.setNull(7, java.sql.Types.DOUBLE)
                    ps.setNull(8, java.sql.Types.DOUBLE)
                    ps.setNull(9, java.sql.Types.DOUBLE)
                    ps.setNull(10, java.sql.Types.FLOAT)
                    ps.setNull(11, java.sql.Types.FLOAT)
                }
                ps.setString(12, r.resultJson)
                ps.setString(13, r.status)
                ps.executeUpdate()
            }
        }
    }

    fun findByEventId(eventId: UUID): List<Restoration> {
        ds.connection.use { c ->
            c.prepareStatement("SELECT * FROM restorations WHERE event_id = ? ORDER BY timestamp DESC").use { ps ->
                ps.setBytes(1, uuidToBytes(eventId))
                ps.executeQuery().use { rs ->
                    val list = mutableListOf<Restoration>()
                    while (rs.next()) {
                        val world = rs.getString("world")
                        val loc = if (world != null) LocationData(
                            world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                            rs.getFloat("yaw"), rs.getFloat("pitch")
                        ) else null
                        list.add(
                            Restoration(
                                bytesToUuid(rs.getBytes("restoration_id")),
                                bytesToUuid(rs.getBytes("event_id")),
                                bytesToUuid(rs.getBytes("admin_uuid")),
                                bytesToUuid(rs.getBytes("target_uuid")),
                                rs.getLong("timestamp"),
                                loc,
                                rs.getString("result_json"),
                                rs.getString("status")
                            )
                        )
                    }
                    return list
                }
            }
        }
    }

    fun exists(eventId: UUID): Boolean {
        ds.connection.use { c ->
            c.prepareStatement("SELECT 1 FROM restorations WHERE event_id = ? LIMIT 1").use { ps ->
                ps.setBytes(1, uuidToBytes(eventId))
                ps.executeQuery().use { rs -> return rs.next() }
            }
        }
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
