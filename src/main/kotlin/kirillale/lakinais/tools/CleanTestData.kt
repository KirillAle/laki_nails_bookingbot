package kirillale.lakinais.tools

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.TestDataMarkers
import java.sql.Connection

/**
 * Удаляет моковые данные из DatabaseEntitiesTest и связанные записи.
 *
 * Запуск:
 *   ./gradlew cleanTestData          — предпросмотр
 *   ./gradlew cleanTestData -Pexecute — удаление
 */
fun main(args: Array<String>) {
    val execute = args.contains("--execute") || System.getProperty("execute") == "true"
    DatabaseFactory.init()

    DatabaseFactory.db.useConnection { conn ->
        val counts = countSnapshot(conn)
        printReport(counts, if (execute) "ПЕРЕД УДАЛЕНИЕМ" else "ПРЕДПРОСМОТР (ничего не удалено)")

        if (!execute) {
            println("\nДля удаления: ./gradlew cleanTestData -Pexecute")
            return@useConnection
        }

        conn.autoCommit = false
        try {
            val deleted = deleteTestData(conn)
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

private data class TableCounts(
    val testAccounts: Int,
    val testProcedures: Int,
    val testSchedules: Int,
    val testBlocks: Int,
    val testBookings: Int,
    val testMasterProcedures: Int,
)

private fun testAccountIdsSubquery(): String = """
    SELECT id FROM account_form WHERE ${TestDataMarkers.testAccountTelegramSqlCondition()}
""".trimIndent()

private fun testProcedureIdsSubquery(): String = """
    SELECT id FROM procedure WHERE ${TestDataMarkers.testProcedureSqlCondition()}
""".trimIndent()

private fun testBookingIdsSubquery(): String = """
    SELECT b.id FROM booking b
    LEFT JOIN account_form client ON client.id = b.client_id
    LEFT JOIN master_schedule ms ON ms.id = b.schedule_id
    LEFT JOIN account_form master ON master.id = ms.master_id
    WHERE b.client_id IN (${testAccountIdsSubquery()})
       OR ms.master_id IN (${testAccountIdsSubquery()})
       OR b.procedure_id IN (${testProcedureIdsSubquery()})
""".trimIndent()

private fun countSnapshot(conn: Connection): TableCounts = TableCounts(
    testAccounts = scalar(conn, "SELECT COUNT(*) FROM account_form WHERE ${TestDataMarkers.testAccountTelegramSqlCondition()}"),
    testProcedures = scalar(conn, "SELECT COUNT(*) FROM procedure WHERE ${TestDataMarkers.testProcedureSqlCondition()}"),
    testSchedules = scalar(conn, """
        SELECT COUNT(*) FROM master_schedule WHERE master_id IN (${testAccountIdsSubquery()})
    """.trimIndent()),
    testBlocks = scalar(conn, """
        SELECT COUNT(*) FROM master_time_block WHERE master_id IN (${testAccountIdsSubquery()})
    """.trimIndent()),
    testBookings = scalar(conn, "SELECT COUNT(*) FROM (${testBookingIdsSubquery()}) t"),
    testMasterProcedures = scalar(conn, """
        SELECT COUNT(*) FROM master_procedure
        WHERE master_id IN (${testAccountIdsSubquery()})
           OR procedure_id IN (${testProcedureIdsSubquery()})
    """.trimIndent()),
)

private fun deleteTestData(conn: Connection): Map<String, Int> {
    val bookings = execDelete(conn, "DELETE FROM booking WHERE id IN (${testBookingIdsSubquery()})")
    val blocks = execDelete(conn, """
        DELETE FROM master_time_block WHERE master_id IN (${testAccountIdsSubquery()})
    """.trimIndent())
    val masterProcedures = execDelete(conn, """
        DELETE FROM master_procedure
        WHERE master_id IN (${testAccountIdsSubquery()})
           OR procedure_id IN (${testProcedureIdsSubquery()})
    """.trimIndent())
    val schedules = execDelete(conn, """
        DELETE FROM master_schedule WHERE master_id IN (${testAccountIdsSubquery()})
    """.trimIndent())
    val procedures = execDelete(conn, """
        DELETE FROM procedure WHERE id IN (${testProcedureIdsSubquery()})
    """.trimIndent())
    val accounts = execDelete(conn, """
        DELETE FROM account_form WHERE ${TestDataMarkers.testAccountTelegramSqlCondition()}
    """.trimIndent())

    return mapOf(
        "booking" to bookings,
        "master_time_block" to blocks,
        "master_procedure" to masterProcedures,
        "master_schedule" to schedules,
        "procedure" to procedures,
        "account_form" to accounts,
    )
}

private fun printReport(counts: TableCounts, title: String) {
    println("\n=== $title ===")
    println("  тестовые аккаунты:         ${counts.testAccounts}")
    println("  тестовые процедуры:        ${counts.testProcedures}")
    println("  расписания тест-мастеров:  ${counts.testSchedules}")
    println("  блокировки тест-мастеров:  ${counts.testBlocks}")
    println("  тестовые бронирования:     ${counts.testBookings}")
    println("  master_procedure (тест):   ${counts.testMasterProcedures}")
}

private fun scalar(conn: Connection, sql: String): Int =
    conn.createStatement().executeQuery(sql).use { rs ->
        rs.next()
        rs.getInt(1)
    }

private fun execDelete(conn: Connection, sql: String): Int =
    conn.createStatement().executeUpdate(sql)
