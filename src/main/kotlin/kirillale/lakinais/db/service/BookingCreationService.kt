package kirillale.lakinais.db.service

import kirillale.lakinais.booking.BookingIntervals
import kirillale.lakinais.booking.BookingSlotMode
import kirillale.lakinais.bot.BotProcedureOption
import kirillale.lakinais.db.entities.BookingEntity
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class BookingPlanItem(
    val option: BotProcedureOption,
    val procedureId: UUID,
    val scheduleId: UUID,
    val startTime: Instant,
    val price: BigDecimal,
)

class BookingCreationService(
    private val bookingService: BookingService = BookingService(),
    private val procedureService: ProcedureService = ProcedureService(),
) {
    companion object {
        const val MAX_ACTIVE_BOOKINGS_PER_CLIENT = 3
        private const val SLOT_MINUTES = 15
    }

    fun countActiveBookings(clientId: UUID): Int =
        bookingService.getClientBookings(clientId).count {
            it.statusName == "PENDING" || it.statusName == "CONFIRMED"
        }

    fun buildPlan(
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        scheduleId: UUID,
        slotStart: Instant,
    ): List<BookingPlanItem> {
        require(procedures.isNotEmpty())

        return when {
            procedures.size == 1 -> {
                listOf(planItem(procedures.first(), scheduleId, slotStart))
            }
            mode == BookingSlotMode.SPLIT -> {
                val active = splitPriority ?: procedures.first()
                listOf(planItem(active, scheduleId, slotStart))
            }
            else -> {
                val manicure = procedures.first { it.procedureType == "Маникюр" }
                val pedicure = procedures.first { it.procedureType == "Педикюр" }
                val pedicureStart = slotStart.plusSeconds(manicure.durationSlots * SLOT_MINUTES * 60L)
                listOf(
                    planItem(manicure, scheduleId, slotStart),
                    planItem(pedicure, scheduleId, pedicureStart),
                )
            }
        }
    }

    fun createFromPlan(clientId: UUID, plan: List<BookingPlanItem>): List<BookingEntity> {
        if (countActiveBookings(clientId) + plan.size > MAX_ACTIVE_BOOKINGS_PER_CLIENT) {
            throw BookingLimitExceededException(
                "У вас уже есть активные записи (максимум $MAX_ACTIVE_BOOKINGS_PER_CLIENT). " +
                    "Отмените одну из них или дождитесь визита.",
            )
        }
        val reservedInPlan = mutableListOf<Pair<Instant, Instant>>()
        val created = mutableListOf<BookingEntity>()
        for (item in plan) {
            if (!bookingService.isSlotAvailable(
                    scheduleId = item.scheduleId,
                    startTime = item.startTime,
                    durationSlots = item.option.durationSlots,
                    extraOccupied = reservedInPlan,
                )) {
                throw SlotTakenException("Окно ${item.startTime} уже занято. Выберите другое время.")
            }
            val booking = bookingService.createBooking(
                clientId = clientId,
                scheduleId = item.scheduleId,
                procedureId = item.procedureId,
                initialStatus = "PENDING",
                priceSnapshot = item.price,
                startTime = item.startTime,
                extraOccupied = reservedInPlan,
            )
            reservedInPlan.add(
                item.startTime to BookingIntervals.slotEnd(item.startTime, item.option.durationSlots),
            )
            created.add(booking)
        }
        return created
    }

    private fun planItem(
        option: BotProcedureOption,
        scheduleId: UUID,
        startTime: Instant,
    ): BookingPlanItem {
        val procedure = procedureService.getByTypeAndSubtype(option.procedureType, option.procedureSubtype)
            ?: throw IllegalStateException("Процедура не найдена в БД: ${option.procedureType} / ${option.procedureSubtype}")
        return BookingPlanItem(
            option = option,
            procedureId = procedure.id,
            scheduleId = scheduleId,
            startTime = startTime,
            price = BigDecimal(procedure.price),
        )
    }
}

class BookingLimitExceededException(message: String) : Exception(message)
class SlotTakenException(message: String) : Exception(message)
