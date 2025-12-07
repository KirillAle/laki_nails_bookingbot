import dev.inmo.micro_utils.coroutines.subscribe
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.extensions.utils.shortcuts.textMessages
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.longPolling
import dev.inmo.tgbotapi.types.buttons.ReplyKeyboardMarkup
import dev.inmo.tgbotapi.types.buttons.SimpleKeyboardButton
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun Application.configureApplicationTgBot() {
    val botToken: String =
        environment.config.propertyOrNull("telegram.botToken")?.getString()
        ?: System.getenv("LAKI_NAILS_BOT_TOKEN")
        ?: error("Telegram bot token is not set")

    val bot = telegramBot(botToken)

    val scope = CoroutineScope(Dispatchers.Default)


    scope.launch {
        bot.longPolling {
            textMessages().subscribe(scope) { message ->
                println(message.chat)

//                 Для отображения имени пользователя, который пищет сообщение
                val user = message.from
                val userName = user?.username?.username ?: "без имени"
                val firstName = user?.firstName ?: ""
                val lastName = user?.lastName ?: ""
                val  displayName = when {
                    userName != "без имени" -> "$userName"
                    else -> "$firstName $lastName".trim()
                }

                bot.sendMessage(
                    message.chat.id,
                     "Пользователь $displayName написали: ${message.content.text}")

                val text = message.content.text

                if (text == "/start") {
                    bot.sendMessage(
                        chatId = message.chat.id,
                        text = "Выбери услугу:",
                        replyMarkup = ReplyKeyboardMarkup(
                            keyboard = listOf(
                                listOf(
                                    SimpleKeyboardButton("\uD83D\uDC85 Маникюр"),
                                    SimpleKeyboardButton("\uD83E\uDDB6 Педикюр"),
                                )
                            ),
                            resizeKeyboard = true
                        )
                    )
                    return@subscribe
                }
            }
        }
    }
}

