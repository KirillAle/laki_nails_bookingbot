package kirillale.lakinais.db.support

import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.entities.ProcedureEntity
import kirillale.lakinais.db.repositiries.AccountRepository
import kirillale.lakinais.db.repositiries.BookingRepository
import kirillale.lakinais.db.repositiries.MasterScheduleRepository
import kirillale.lakinais.db.repositiries.ProcedureRepository
import kirillale.lakinais.db.service.BookingService
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Изолированный рабочий день для интеграционных тестов бронирования.
 * telegram_id с префиксом overlap_test_ — для [kirillale.lakinais.db.TestDataMarkers].
 */
class BookingTestFixture(
    val zoneId: ZoneId = ZoneId.of("Asia/Tbilisi"),
    val date: LocalDate = LocalDate.of(2099, 6, 25),
) {
    private val suffix = System.currentTimeMillis()
    private val accountRepository = AccountRepository()
    private val scheduleRepository = MasterScheduleRepository()
    private val procedureRepository = ProcedureRepository()
    private val bookingRepository = BookingRepository()
    private val bookingService = BookingService()

    val masterId: UUID
    val clientId: UUID
    val schedule: MasterScheduleEntity
    val procedureFourSlots: ProcedureEntity
    val procedureSixSlots: ProcedureEntity

    private val bookingIds = mutableListOf<UUID>()

    init {
        DatabaseFactory.init()
        MasterScheduleRepository().ensureSchema()
        val master = accountRepository.createAccount(
            telegramId = "overlap_test_master_$suffix",
            firstName = "Overlap",
            lastName = "Master",
            userName = "overlap_master_$suffix",
            role = "MASTER",
        )
        val client = accountRepository.createAccount(
            telegramId = "overlap_test_client_$suffix",
            firstName = "Overlap",
            lastName = "Client",
            userName = "overlap_client_$suffix",
            role = "CLIENT",
        )
        masterId = master.id
        clientId = client.id

        val dayInstant = SalonTime.dayInstant(date, zoneId)
        val workStart = SalonTime.atTime(date, LocalTime.of(10, 0), zoneId)
        val workEnd = SalonTime.atTime(date, LocalTime.of(19, 0), zoneId)
        val noBreak = workStart
        schedule = scheduleRepository.createSchedule(
            masterId = masterId,
            date = dayInstant,
            timeStart = workStart,
            timeEnd = workEnd,
            breakStart = noBreak,
            breakEnd = noBreak,
        )

        procedureFourSlots = procedureRepository.createProcedure(
            procedureType = "Тест",
            procedureSubtype = "Overlap4_$suffix",
            durationSlot = 4,
            price = 100,
            isActive = true,
        )
        procedureSixSlots = procedureRepository.createProcedure(
            procedureType = "Тест",
            procedureSubtype = "Overlap6_$suffix",
            durationSlot = 6,
            price = 100,
            isActive = true,
        )
    }

    fun atTime(time: LocalTime): Instant = SalonTime.atTime(date, time, zoneId)

    fun createBooking(
        start: LocalTime,
        procedure: ProcedureEntity = procedureSixSlots,
        status: String = "CONFIRMED",
    ): BookingEntity {
        val entity = bookingService.createBooking(
            clientId = clientId,
            scheduleId = schedule.id,
            procedureId = procedure.id,
            initialStatus = status,
            priceSnapshot = BigDecimal("100"),
            startTime = atTime(start),
        )
        bookingIds += entity.id
        val persisted = bookingRepository.findById(entity.id)
            ?: error("Booking not persisted: ${entity.id}")
        checkNotNull(persisted.startTime) { "start_time not persisted for booking ${entity.id}" }
        return entity
    }

    fun isAvailable(start: LocalTime, durationSlots: Int): Boolean =
        bookingService.isSlotAvailable(
            scheduleId = schedule.id,
            startTime = atTime(start),
            durationSlots = durationSlots,
        )

    fun cleanup() {
        bookingIds.forEach { bookingRepository.deleteById(it) }
        scheduleRepository.deleteById(schedule.id)
    }
}
