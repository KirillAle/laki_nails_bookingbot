package kirillale.lakinais.tools

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.TestDataMarkers
import kirillale.lakinais.db.service.MasterResolver
import java.sql.ResultSet

fun main() {
    DatabaseFactory.init()
    DatabaseFactory.db.useConnection { conn ->
        println("=== URL: ${conn.metaData.url} ===")

        ensurePhoneColumn(conn)

        println("\n=== TABLES (public) ===")
        conn.createStatement().executeQuery(
            """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public' ORDER BY table_name
            """.trimIndent(),
        ).forEachRow { println("  - ${it.getString(1)}") }

        println("\n=== account_form (${count(conn, "account_form")} rows) ===")
        conn.createStatement().executeQuery(
            """
            SELECT telegram_id, first_name, last_name, user_name, role, phone, created_at
            FROM account_form ORDER BY created_at
            """.trimIndent(),
        ).forEachRow {
            val tg = it.getString(1)
            val source = when {
                TestDataMarkers.isTestTelegramId(tg) -> "TEST (DatabaseEntitiesTest)"
                else -> "REAL (bot /start)"
            }
            println(
                "  [$source] tg=$tg name=${it.getString(2)} ${it.getString(3)} " +
                    "role=${it.getString(5)} phone=${it.getString(6)}",
            )
        }

        println("\n=== procedure (${count(conn, "procedure")} rows) ===")
        conn.createStatement().executeQuery(
            "SELECT procedure_type, procedure_subtype, is_active FROM procedure ORDER BY procedure_type, procedure_subtype",
        ).forEachRow {
            println("  ${it.getString(1)} / ${it.getString(2)} active=${it.getBoolean(3)}")
        }

        println("\n=== master_schedule (${count(conn, "master_schedule")} rows) ===")
        conn.createStatement().executeQuery(
            """
            SELECT ms.date, ms.time_start, ms.time_end, af.telegram_id
            FROM master_schedule ms
            LEFT JOIN account_form af ON af.id = ms.master_id
            ORDER BY ms.date
            """.trimIndent(),
        ).forEachRow {
            println("  ${it.getDate(1)} ${it.getTime(2)}-${it.getTime(3)} master=${it.getString(4)}")
        }

        println("\n=== booking (${count(conn, "booking")} rows) ===")
        conn.createStatement().executeQuery(
            """
            SELECT b.status, b.start_time, af.telegram_id
            FROM booking b
            LEFT JOIN account_form af ON af.id = b.client_id
            ORDER BY b.start_time
            """.trimIndent(),
        ).forEachRow {
            println("  ${it.getString(1)} ${it.getTimestamp(2)} client=${it.getString(3)}")
        }

        val masters = conn.createStatement().executeQuery(
            "SELECT id, telegram_id, role FROM account_form WHERE role IN ('MASTER','ADMIN','OWNER')",
        )
        println("\n=== staff accounts ===")
        masters.forEachRow {
            println("  id=${it.getString(1)} tg=${it.getString(2)} role=${it.getString(3)}")
        }

        val masterId = MasterResolver().resolveMasterId()
        println("\n=== resolved master for booking ===")
        conn.prepareStatement(
            "SELECT telegram_id, first_name, role FROM account_form WHERE id = ?",
        ).use { ps ->
            ps.setObject(1, masterId)
            ps.executeQuery().forEachRow {
                println("  id=$masterId tg=${it.getString(1)} name=${it.getString(2)} role=${it.getString(3)}")
            }
        }
        conn.prepareStatement(
            "SELECT COUNT(*) FROM master_schedule WHERE master_id = ? AND date >= CURRENT_DATE",
        ).use { ps ->
            ps.setObject(1, masterId)
            ps.executeQuery().forEachRow {
                println("  open/future schedule days: ${it.getInt(1)}")
            }
        }
    }
}

private fun ensurePhoneColumn(conn: java.sql.Connection) {
    val hasPhone = conn.createStatement().executeQuery(
        """
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'account_form' AND column_name = 'phone'
        """.trimIndent(),
    ).use { rs -> rs.next() }

    if (!hasPhone) {
        println("Applying migration: add phone column to account_form...")
        conn.createStatement().execute("ALTER TABLE account_form ADD COLUMN IF NOT EXISTS phone VARCHAR(32)")
        println("Migration applied.")
    } else {
        println("Column account_form.phone already exists.")
    }
}

private fun count(conn: java.sql.Connection, table: String): Int =
    conn.createStatement().executeQuery("SELECT COUNT(*) FROM $table").use { rs ->
        rs.next()
        rs.getInt(1)
    }

private inline fun ResultSet.forEachRow(block: (ResultSet) -> Unit) {
    while (next()) block(this)
}
