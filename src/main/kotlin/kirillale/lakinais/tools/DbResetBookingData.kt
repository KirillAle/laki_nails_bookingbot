package kirillale.lakinais.tools

import kirillale.lakinais.db.DatabaseFactory
import java.sql.Connection

/**
 * Полная очистка расписаний и записей (аккаунты и каталог procedure не трогаем).
 *
 *   ./gradlew dbResetBookingData          — предпросмотр
 *   ./gradlew dbResetBookingData -Pexecute — удаление
 */
fun main(args: Array<String>) {
    val execute = args.contains("--execute") || System.getProperty("execute") == "true"
    DatabaseFactory.init()

    DatabaseFactory.db.useConnection { conn ->
        ensureReminderTable(conn)
        val counts = countSnapshot(conn)
        printReport(counts, if (execute) "ПЕРЕД УДАЛЕНИЕМ" else "ПРЕДПРОСМОТР (ничего не удалено)")

        if (!execute) {
            println("\nДля удаления: ./gradlew dbResetBookingData -Pexecute")
            return@useConnection
        }

        conn.autoCommit = false
        try {
            val deleted = deleteBookingData(conn)
            conn.commit()
            println("\n=== Удалено ===")
            deleted.forEach { (table, n) -> println("  $table: $n") }
            printReport(countSnapshot(conn), "ПОСЛЕ ОЧИСТКИ")
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }
}

private data class BookingDataCounts(
    val bookings: Int,
    val schedules: Int,
    val blocks: Int,
    val reminders: Int,
)

private fun ensureReminderTable(conn: Connection) {
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

private fun countSnapshot(conn: Connection): BookingDataCounts = BookingDataCounts(
    bookings = scalar(conn, "SELECT COUNT(*) FROM booking"),
    schedules = scalar(conn, "SELECT COUNT(*) FROM master_schedule"),
    blocks = scalar(conn, "SELECT COUNT(*) FROM master_time_block"),
    reminders = scalar(conn, "SELECT COUNT(*) FROM booking_reminder"),
)

private fun deleteBookingData(conn: Connection): Map<String, Int> {
    val reminders = execDelete(conn, "DELETE FROM booking_reminder")
    val bookings = execDelete(conn, "DELETE FROM booking")
    val blocks = execDelete(conn, "DELETE FROM master_time_block")
    val schedules = execDelete(conn, "DELETE FROM master_schedule")
    return mapOf(
        "booking_reminder" to reminders,
        "booking" to bookings,
        "master_time_block" to blocks,
        "master_schedule" to schedules,
    )
}

private fun printReport(counts: BookingDataCounts, title: String) {
    println("\n=== $title ===")
    println("  booking:            ${counts.bookings}")
    println("  master_schedule:    ${counts.schedules}")
    println("  master_time_block:  ${counts.blocks}")
    println("  booking_reminder:   ${counts.reminders}")
}

private fun scalar(conn: Connection, sql: String): Int =
    conn.createStatement().executeQuery(sql).use { rs ->
        rs.next()
        rs.getInt(1)
    }

private fun execDelete(conn: Connection, sql: String): Int =
    conn.createStatement().executeUpdate(sql)
