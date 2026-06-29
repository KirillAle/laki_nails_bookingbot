package kirillale.lakinais.db

import kirillale.lakinais.bot.BotProcedureCatalog

/**
 * Маркеры данных, созданных интеграционными тестами (overlap_test_*, legacy test_*).
 */
object TestDataMarkers {

    private val catalogSubtypes = BotProcedureCatalog.all()
        .map { it.procedureType to it.procedureSubtype }
        .toSet()

    fun isTestTelegramId(telegramId: String): Boolean {
        val tg = telegramId.trim()
        return tg.startsWith("overlap_test_") ||
            tg.startsWith("test_telegram_") ||
            tg.startsWith("service_test_") ||
            tg.startsWith("master_proc_") ||
            tg.startsWith("master_block_") ||
            tg.startsWith("master_booking_") ||
            tg.startsWith("client_booking_") ||
            (tg.startsWith("master_") && tg.removePrefix("master_").all { it.isDigit() })
    }

    fun isTestProcedure(type: String, subtype: String): Boolean {
        if (subtype.startsWith("Overlap4_") || subtype.startsWith("Overlap6_")) return true
        if (type == "Тест" && subtype == "Тест") return true
        if (subtype == "Классический") return true
        return (type to subtype) !in catalogSubtypes &&
            (type == "Маникюр" || type == "Педикюр" || type == "Тест")
    }

    /** SQL-фрагмент: telegram_id тестового аккаунта. */
    fun testAccountTelegramSqlCondition(column: String = "telegram_id"): String = """
        $column LIKE 'overlap_test_%'
        OR $column LIKE 'test_telegram_%'
        OR $column LIKE 'service_test_%'
        OR $column LIKE 'master_proc_%'
        OR $column LIKE 'master_block_%'
        OR $column LIKE 'master_booking_%'
        OR $column LIKE 'client_booking_%'
        OR ($column LIKE 'master_%' AND $column ~ '^master_[0-9]+$')
    """.trimIndent()

    /** SQL-фрагмент: тестовая процедура (не из каталога бота). */
    fun testProcedureSqlCondition(): String = """
        (procedure_type = 'Тест' AND procedure_subtype = 'Тест')
        OR procedure_subtype = 'Классический'
    """.trimIndent()
}
