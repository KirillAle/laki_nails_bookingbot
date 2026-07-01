package kirillale.lakinais.bot

/** Короткая подпись inline-кнопки: одна строка, без обрезки на мобильном. */
object BotProcedureLabels {
    private const val SELECTED_MARK = "✓ "

    fun inlineButtonLabel(option: BotProcedureOption, selected: Boolean): String =
        if (selected) SELECTED_MARK + option.keyboardLabel else option.keyboardLabel
}
