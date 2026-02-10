package kirillale.lakinais.db

import io.github.cdimascio.dotenv.dotenv
import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.tables.AccountFormTable
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.tables.BookingTable
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.tables.MasterScheduleTable
import kirillale.lakinais.db.entities.ProcedureEntity
import kirillale.lakinais.db.tables.ProcedureTable
import kirillale.lakinais.db.entities.MasterProcedureEntity
import kirillale.lakinais.db.tables.MasterProcedureTable
import kirillale.lakinais.db.entities.MasterTimeBlockEntity
import kirillale.lakinais.db.tables.MasterTimeBlockTable
import kirillale.lakinais.db.entities.TimeSlotEntity
import kirillale.lakinais.db.tables.TimeSlotTable
import org.ktorm.database.Database
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList

object DatabaseFactory {
    private val dotenv = try {
        // Пытаемся найти файл environment.env в корне проекта
        val projectRoot = System.getProperty("user.dir")
        dotenv {
            directory = projectRoot
            filename = "environment.env"
            ignoreIfMissing = true
        }
    } catch (e: Exception) {
        // Если не удалось загрузить, пробуем из текущей директории
        try {
            dotenv {
                directory = "./"
                filename = "environment.env"
                ignoreIfMissing = true
            }
        } catch (e2: Exception) {
            null
        }
    }

    private fun getEnv(key: String): String? = System.getenv(key) ?: dotenv?.get(key)

    private val DATABASE_URL = getEnv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    private val DATABASE_USER = getEnv("DATABASE_USER") ?: getEnv("DATABASE_NAME") ?: "postgres"
    private val DATABASE_PASSWORD = getEnv("DATABASE_PASSWORD") ?: ""

    val db by lazy {
        Database.connect(
            url = DATABASE_URL,
            driver = "org.postgresql.Driver",
            user = DATABASE_USER,
            password = DATABASE_PASSWORD
        )
    }

    fun select() {
        try {
            val accountModel: List<AccountFormEntity> = db.sequenceOf(AccountFormTable).toList()
            println("Account models: ${accountModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении AccountFormTable: ${e.message}")
        }

        try {
            val bookingModel: List<BookingEntity> = db.sequenceOf(BookingTable).toList()
            println("Booking models: ${bookingModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении BookingTable: ${e.message}")
        }

        try {
            val masterScheduleModel: List<MasterScheduleEntity> = db.sequenceOf(MasterScheduleTable).toList()
            println("MasterSchedule models: ${masterScheduleModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении MasterScheduleTable: ${e.message}")
        }

        try {
            val procedureModel: List<ProcedureEntity> = db.sequenceOf(ProcedureTable).toList()
            println("Procedure models: ${procedureModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении ProcedureTable: ${e.message}")
        }

        try {
            val masterProcedureModel: List<MasterProcedureEntity> = db.sequenceOf(MasterProcedureTable).toList()
            println("MasterProcedure models: ${masterProcedureModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении MasterProcedureTable: ${e.message}")
        }

        try {
            val masterTimeBlockModel: List<MasterTimeBlockEntity> = db.sequenceOf(MasterTimeBlockTable).toList()
            println("MasterTimeBlock models: ${masterTimeBlockModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении MasterTimeBlockTable: ${e.message}")
        }

        try {
            val timeSlotModel: List<TimeSlotEntity> = db.sequenceOf(TimeSlotTable).toList()
            println("TimeSlot models: ${timeSlotModel.joinToString { it.id.toString() }}")
        } catch (e: Exception) {
            println("Ошибка при чтении TimeSlotTable: ${e.message}")
        }

        println("DATABASE_URL: $DATABASE_URL")
        println("DATABASE_USER: $DATABASE_USER")
    }
}