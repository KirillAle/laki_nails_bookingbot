package kirillale.lakinais.bot

import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton

object BotUi {
    const val START_BUTTON = "Приступить к выбору процедуры"
    const val MASTER_MENU_BUTTON = "🛠 Меню мастера"

    fun startReplyKeyboard(): ReplyKeyboardMarkup = ReplyKeyboardMarkup(
        keyboard = listOf(listOf(SimpleKeyboardButton(START_BUTTON))),
        resizeKeyboard = true,
    )

    fun staffClientReplyKeyboard(): ReplyKeyboardMarkup = ReplyKeyboardMarkup(
        keyboard = listOf(
            listOf(SimpleKeyboardButton(START_BUTTON)),
            listOf(SimpleKeyboardButton(MASTER_MENU_BUTTON)),
        ),
        resizeKeyboard = true,
    )
}
