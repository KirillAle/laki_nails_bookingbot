package kirillale.lakinais.db

import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.tables.AccountFormTable
import org.ktorm.database.Database
import org.ktorm.entity.Entity
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import org.ktorm.schema.Table
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar
import java.time.Instant
import java.util.UUID

object DatabaseFactory {
    private val DATABASE_URL = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    private val DATABASE_USER = System.getenv("DATABASE_USER") ?: System.getenv("DATABASE_NAME") ?: "postgres"
    private val DATABASE_PASSWORD = System.getenv("DATABASE_PASSWORD") ?: ""

    val db = Database.connect(
        url = DATABASE_URL,
        driver = "org.postgresql.Driver",
        user = DATABASE_USER,
        password = DATABASE_PASSWORD
    )

    fun select() {
        try {
            val accountModel: List<AccountFormEntity> = db.sequenceOf(AccountFormTable).toList()
            println("Account models: ${accountModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении AccountFormTable: ${e.message}")
        }

        try {
            val bookingModel: List<BookingModel> = db.sequenceOf(BookingTable).toList()
            println("Booking models: ${bookingModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении BookingTable: ${e.message}")
        }

        try {
            val masterScheduleModel: List<MasterScheduleModel> = db.sequenceOf(MasterScheduleTable).toList()
            println("MasterSchedule models: ${masterScheduleModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении MasterScheduleTable: ${e.message}")
        }

        println("DATABASE_URL: $DATABASE_URL")
        println("DATABASE_USER: $DATABASE_USER")
    }



    interface BookingModel : Entity<BookingModel> {
        val id: UUID
        val clientId: UUID
        val masterId: UUID
        val scheduleId: UUID
        val procedureId: UUID
        val statusName: String
        val createdAt: Instant?
    }

    interface MasterScheduleModel: Entity<MasterScheduleModel> {
        val id: UUID
        val masterId: UUID
        val date: Instant
        val timeStart: Instant
        val timeEnd: Instant
        val breakStart: Instant
        val breakEnd: Instant
    }

    interface ProcedureModel: Entity<ProcedureModel> {
        val id: UUID
        val procedureType: String
        val procedureSubtype: String
        val durationSlot: Int
    }



    object BookingTable: Table<BookingModel>("booking"){
        val id = uuid("id").bindTo { it.id }
        val client_id = uuid("client_id").bindTo { it.clientId }
        val master_id = uuid("master_id").bindTo { it.masterId }
        val schedule_id = uuid("schedule_id").bindTo { it.scheduleId }
        val procedure_id = uuid("procedure_id").bindTo { it.procedureId}
        val status = varchar("status").bindTo { it.statusName }
        val created_at = timestamp ("created_at").bindTo { it.createdAt }
    }

    object MasterScheduleTable: Table<MasterScheduleModel>("master_schedule"){
        val id = uuid("id").bindTo { it.id }
        val master_id = uuid("master_id").bindTo { it.masterId }
        val date = timestamp("date").bindTo { it.date }
        val time_start = timestamp("time_start").bindTo { it.timeStart}
        val time_end = timestamp("time_end").bindTo { it.timeEnd}
        val break_start = timestamp("break_start").bindTo { it.breakStart }
        val break_end = timestamp("break_end").bindTo { it.breakEnd }
    }

}