package kirillale.lakinais.bot

import kirillale.lakinais.booking.BookingSlotMode
import kirillale.lakinais.booking.DateInterval
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private enum class FlowStep {
    BROWSING,
    AWAITING_PHONE,
    AWAITING_CONFIRM,
}

data class ChatBookingState(
    val selected: MutableList<BotProcedureOption> = mutableListOf(),
    var mode: BookingSlotMode = BookingSlotMode.CONSECUTIVE,
    var splitPriority: BotProcedureOption? = null,
    var splitFirstDone: Boolean = false,
    var currentInterval: DateInterval? = null,
    var pendingScheduleId: UUID? = null,
    var pendingSlotStart: Instant? = null,
)

object BookingFlowState {
    private val byChat = ConcurrentHashMap<String, ChatBookingState>()
    private val stepByChat = ConcurrentHashMap<String, FlowStep>()

    fun getOrCreate(chatId: String): ChatBookingState =
        byChat.getOrPut(chatId) { ChatBookingState() }

    fun getSelection(chatId: String): List<BotProcedureOption> =
        byChat[chatId]?.selected?.toList().orEmpty()

    fun isAwaitingPhone(chatId: String): Boolean =
        stepByChat[chatId] == FlowStep.AWAITING_PHONE

    fun hasPendingBooking(chatId: String): Boolean {
        val state = byChat[chatId] ?: return false
        return state.pendingScheduleId != null && state.pendingSlotStart != null
    }

    fun toggle(chatId: String, option: BotProcedureOption): List<BotProcedureOption> {
        val state = getOrCreate(chatId)
        val list = state.selected
        synchronized(list) {
            val idx = list.indexOfFirst { it.buttonText == option.buttonText }
            if (idx >= 0) {
                list.removeAt(idx)
            } else {
                val sameTypeIdx = list.indexOfFirst { it.procedureType == option.procedureType }
                if (sameTypeIdx >= 0) {
                    list[sameTypeIdx] = option
                } else if (list.size < 2) {
                    list.add(option)
                }
            }
            resetBookingProgress(chatId, state)
            return list.toList()
        }
    }

    fun clear(chatId: String) {
        byChat.remove(chatId)
        stepByChat.remove(chatId)
        staffClientModeByChat.remove(chatId)
    }

    fun enableStaffClientMode(chatId: String) {
        staffClientModeByChat[chatId] = true
    }

    fun isStaffClientMode(chatId: String): Boolean =
        staffClientModeByChat[chatId] == true

    private val staffClientModeByChat = ConcurrentHashMap<String, Boolean>()

    fun enableSplit(chatId: String) {
        val state = getOrCreate(chatId)
        state.mode = BookingSlotMode.SPLIT
        state.splitPriority = null
        state.splitFirstDone = false
        state.currentInterval = null
        clearPending(chatId, state)
    }

    fun setSplitPriority(chatId: String, priority: BotProcedureOption) {
        val state = getOrCreate(chatId)
        state.splitPriority = priority
        state.currentInterval = null
        clearPending(chatId, state)
    }

    fun setCurrentInterval(chatId: String, interval: DateInterval) {
        getOrCreate(chatId).currentInterval = interval
    }

    fun setPendingSlot(chatId: String, scheduleId: UUID, slotStart: Instant) {
        val state = getOrCreate(chatId)
        state.pendingScheduleId = scheduleId
        state.pendingSlotStart = slotStart
        stepByChat[chatId] = FlowStep.BROWSING
    }

    fun setAwaitingPhone(chatId: String) {
        stepByChat[chatId] = FlowStep.AWAITING_PHONE
    }

    fun setAwaitingConfirm(chatId: String) {
        stepByChat[chatId] = FlowStep.AWAITING_CONFIRM
    }

    fun clearPending(chatId: String) {
        val state = byChat[chatId] ?: return
        clearPending(chatId, state)
    }

    fun markSplitFirstDone(chatId: String) {
        val state = getOrCreate(chatId)
        state.splitFirstDone = true
        state.splitPriority = state.selected.firstOrNull { it != state.splitPriority }
        state.currentInterval = null
        clearPending(chatId, state)
    }

    private fun resetBookingProgress(chatId: String, state: ChatBookingState) {
        state.mode = BookingSlotMode.CONSECUTIVE
        state.splitPriority = null
        state.splitFirstDone = false
        state.currentInterval = null
        clearPending(chatId, state)
    }

    private fun clearPending(chatId: String, state: ChatBookingState) {
        state.pendingScheduleId = null
        state.pendingSlotStart = null
        stepByChat[chatId] = FlowStep.BROWSING
    }
}
