package kirillale.lakinais.bot

/**
 * Одна процедура для кнопок бота.
 * @param buttonText текст на кнопке (название + время работы)
 * @param durationSlots длительность для бронирования слота: работа + перерыв мастера, в 15-мин слотах
 * @param procedureType тип процедуры для таблицы procedure
 * @param procedureSubtype подтип для таблицы procedure
 */
data class BotProcedureOption(
    val buttonText: String,
    val durationSlots: Int,
    val procedureType: String,
    val procedureSubtype: String
)

/**
 * Каталог из 8 процедур для выбора в боте.
 * Время на кнопке — только работа; в логике слотов учитывается работа + перерыв.
 */
object BotProcedureCatalog {

    private val list = listOf(
        // 1. маникюр гигиенический 45 мин + 15 мин перерыв → 60 мин = 4 слота
        BotProcedureOption(
            buttonText = "Маникюр гигиенический 45 мин",
            durationSlots = 4,
            procedureType = "Маникюр",
            procedureSubtype = "Гигиенический"
        ),
        // 2. педикюр экспресс 45 мин + 15 мин перерыв → 60 мин = 4 слота
        BotProcedureOption(
            buttonText = "Педикюр экспресс 45 мин",
            durationSlots = 4,
            procedureType = "Педикюр",
            procedureSubtype = "Экспресс (обработка пальчиков)"
        ),
        // 3. маникюр + покрытие 1,5 ч + 30 мин перерыв → 120 мин = 8 слотов
        BotProcedureOption(
            buttonText = "Маникюр + покрытие 90 мин",
            durationSlots = 8,
            procedureType = "Маникюр",
            procedureSubtype = "Покрытие"
        ),
        // 4. педикюр экспресс + гель лак 45 мин + 15 мин перерыв → 60 мин = 4 слота
        BotProcedureOption(
            buttonText = "Педикюр экспресс + гель лак 45 мин",
            durationSlots = 4,
            procedureType = "Педикюр",
            procedureSubtype = "Экспресс + гель лак"
        ),
        // 5. маникюр + наращивание 3 ч + 1 ч перерыв → 240 мин = 16 слотов
        BotProcedureOption(
            buttonText = "Маникюр + наращивание 180 мин",
            durationSlots = 16,
            procedureType = "Маникюр",
            procedureSubtype = "Наращивание"
        ),
        // 6. педикюр смарт 1:15 + 15 мин перерыв → 90 мин = 6 слотов
        BotProcedureOption(
            buttonText = "Педикюр смарт 75 мин",
            durationSlots = 6,
            procedureType = "Педикюр",
            procedureSubtype = "Смарт без покрытия"
        ),
        // 7. маникюр + коррекция наращивания 2,5 ч + 30 мин перерыв → 180 мин = 12 слотов
        BotProcedureOption(
            buttonText = "Маникюр + коррекция 150 мин",
            durationSlots = 12,
            procedureType = "Маникюр",
            procedureSubtype = "Коррекция наращивания"
        ),
        // 8. педикюр смарт + покрытие 1,5 ч + 30 мин перерыв → 120 мин = 8 слотов
        BotProcedureOption(
            buttonText = "Педикюр смарт + покрытие 90 мин",
            durationSlots = 8,
            procedureType = "Педикюр",
            procedureSubtype = "Смарт + покрытие"
        )
    )

    fun all(): List<BotProcedureOption> = list
}
