package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

enum class BookingReminderKind(val dbValue: String) {
    DAYS_3("3d"),
    DAY_1("1d"),
    HOURS_2("2h"),
}

class BookingReminderRepository {

    private val db = DatabaseFactory.db

    fun ensureTable() {
        db.useConnection { conn ->
            conn.createStatement().execute(
                """
                CREATE TABLE IF NOT EXISTS booking_reminder (
                    booking_id UUID NOT NULL,
                    kind VARCHAR(16) NOT NULL,
                    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    PRIMARY KEY (booking_id, kind)
                )
                """.trimIndent(),
            )
        }
    }

    fun wasSent(bookingId: UUID, kind: BookingReminderKind): Boolean {
        db.useConnection { conn ->
            conn.prepareStatement(
                "SELECT 1 FROM booking_reminder WHERE booking_id = ? AND kind = ?",
            ).use { ps ->
                ps.setObject(1, bookingId)
                ps.setString(2, kind.dbValue)
                ps.executeQuery().use { rs -> return rs.next() }
            }
        }
    }

    fun markSent(bookingId: UUID, kind: BookingReminderKind, sentAt: Instant = Instant.now()) {
        db.useConnection { conn ->
            conn.prepareStatement(
                """
                INSERT INTO booking_reminder (booking_id, kind, sent_at)
                VALUES (?, ?, ?)
                ON CONFLICT (booking_id, kind) DO NOTHING
                """.trimIndent(),
            ).use { ps ->
                ps.setObject(1, bookingId)
                ps.setString(2, kind.dbValue)
                ps.setTimestamp(3, Timestamp.from(sentAt))
                ps.executeUpdate()
            }
        }
    }
}
