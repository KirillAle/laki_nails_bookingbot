package kirillale.lakinais.bot

/**
 * Подпись inline-кнопки процедуры: перенос строк, лимит Telegram — 64 символа.
 */
object BotProcedureLabels {
    private const val SELECTED_MARK = "✓ "
    private const val MAX_LINE = 13
    private const val TELEGRAM_BUTTON_LIMIT = 64

    fun inlineButtonLabel(option: BotProcedureOption, selected: Boolean): String {
        val titleLines = wrapLines("${option.categoryIcon} ${option.procedureSubtype}")
        val lines = titleLines + option.workTimeLabel
        var body = lines.joinToString("\n")
        if (body.length > TELEGRAM_BUTTON_LIMIT) {
            body = compactToLimit(option)
        }
        return if (selected) SELECTED_MARK + body else body
    }

    private fun compactToLimit(option: BotProcedureOption): String {
        var maxLine = MAX_LINE
        while (maxLine >= 8) {
            val titleLines = wrapLines("${option.categoryIcon} ${option.procedureSubtype}", maxLine)
            val body = (titleLines + option.workTimeLabel).joinToString("\n")
            if (body.length <= TELEGRAM_BUTTON_LIMIT) return body
            maxLine--
        }
        return "${option.categoryIcon} ${option.procedureSubtype.take(20)}\n${option.workTimeLabel}"
    }

    private fun wrapLines(text: String, maxLineLength: Int = MAX_LINE): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()

        val lines = mutableListOf<String>()
        var current = words.first()
        for (i in 1 until words.size) {
            val word = words[i]
            val candidate = "$current $word"
            if (candidate.length <= maxLineLength) {
                current = candidate
            } else {
                lines.add(current)
                current = word
            }
        }
        lines.add(current)
        return lines
    }
}
