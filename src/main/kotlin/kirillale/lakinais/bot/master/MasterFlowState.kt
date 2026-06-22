package kirillale.lakinais.bot.master

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object MasterFlowState {
    private val openHorizonByChat = ConcurrentHashMap<String, AtomicInteger>()

    fun horizonDays(chatId: String, default: Int): Int =
        openHorizonByChat.getOrPut(chatId) { AtomicInteger(default) }.get()

    fun adjustHorizon(chatId: String, default: Int, delta: Int) {
        val holder = openHorizonByChat.getOrPut(chatId) { AtomicInteger(default) }
        holder.updateAndGet { current -> (current + delta).coerceIn(1, 90) }
    }

    fun setHorizon(chatId: String, days: Int) {
        openHorizonByChat.getOrPut(chatId) { AtomicInteger(days) }.set(days.coerceIn(1, 90))
    }

    fun clear(chatId: String) {
        openHorizonByChat.remove(chatId)
    }
}
