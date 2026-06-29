package kirillale.lakinais.bot

/**
 * Одна процедура для кнопок бота.
 * @param buttonText полное название (подтверждение записи, сообщения)
 * @param workTimeLabel длительность работы на кнопке (без перерыва мастера)
 * @param durationSlots длительность для бронирования слота: работа + перерыв мастера, в 15-мин слотах
 */
data class BotProcedureOption(
    val buttonText: String,
    val workTimeLabel: String,
    val durationSlots: Int,
    val procedureType: String,
    val procedureSubtype: String,
) {
    val categoryIcon: String
        get() = when (procedureType) {
            "Маникюр" -> "💅"
            "Педикюр" -> "🦶"
            else -> "•"
        }

    fun inlineButtonLabel(selected: Boolean): String =
        BotProcedureLabels.inlineButtonLabel(this, selected)
}

object BotProcedureCatalog {

    private val list = listOf(
        BotProcedureOption(
            buttonText = "Маникюр гигиенический 45 мин",
            workTimeLabel = "45 мин",
            durationSlots = 4,
            procedureType = "Маникюр",
            procedureSubtype = "Гигиенический",
        ),
        BotProcedureOption(
            buttonText = "Педикюр экспресс 45 мин",
            workTimeLabel = "45 мин",
            durationSlots = 4,
            procedureType = "Педикюр",
            procedureSubtype = "Экспресс (обработка пальчиков)",
        ),
        BotProcedureOption(
            buttonText = "Маникюр + покрытие 90 мин",
            workTimeLabel = "90 мин",
            durationSlots = 8,
            procedureType = "Маникюр",
            procedureSubtype = "Покрытие",
        ),
        BotProcedureOption(
            buttonText = "Педикюр экспресс + гель лак 45 мин",
            workTimeLabel = "45 мин",
            durationSlots = 4,
            procedureType = "Педикюр",
            procedureSubtype = "Экспресс + гель лак",
        ),
        BotProcedureOption(
            buttonText = "Маникюр + наращивание 180 мин",
            workTimeLabel = "3 ч",
            durationSlots = 16,
            procedureType = "Маникюр",
            procedureSubtype = "Наращивание",
        ),
        BotProcedureOption(
            buttonText = "Педикюр смарт 75 мин",
            workTimeLabel = "75 мин",
            durationSlots = 6,
            procedureType = "Педикюр",
            procedureSubtype = "Смарт без покрытия",
        ),
        BotProcedureOption(
            buttonText = "Маникюр + коррекция 150 мин",
            workTimeLabel = "2,5 ч",
            durationSlots = 12,
            procedureType = "Маникюр",
            procedureSubtype = "Коррекция наращивания",
        ),
        BotProcedureOption(
            buttonText = "Педикюр смарт + покрытие 90 мин",
            workTimeLabel = "90 мин",
            durationSlots = 8,
            procedureType = "Педикюр",
            procedureSubtype = "Смарт + покрытие",
        ),
    )

    fun all(): List<BotProcedureOption> = list
}
