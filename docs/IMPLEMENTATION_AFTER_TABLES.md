# Что сделано после сборки и тестов таблиц

Документ описывает: что мы написали, кто к кому ходит, как связаны бот, сервисы и БД. **REST API в проекте нет** — внешний вход только через Telegram-бота.

---

## 1. Общая картина: кто с кем говорит

```
┌─────────────┐     Telegram      ┌──────────────────────────────────────────────────┐
│   Клиент    │ ◄──────────────►  │  Ktor-приложение (Application.module)             │
│  (Telegram) │   Bot API         │                                                    │
└─────────────┘                   │  configureApplicationTgBot()                        │
                                  │       │                                            │
                                  │       ▼                                            │
                                  │  ┌─────────────────────────────────────────────┐  │
                                  │  │ Бот (Behaviour + Long Polling)               │  │
                                  │  │ • onCommand("start")                         │  │
                                  │  │ • onText("Приступить к выбору процедуры")    │  │
                                  │  │ • onDataCallbackQuery (процедуры, дата)      │  │
                                  │  └─────────────────────────────────────────────┘  │
                                  │       │                                            │
                                  │       │ использует                                 │
                                  │       ▼                                            │
                                  │  ┌─────────────┐    ┌──────────────────────────┐   │
                                  │  │ BotProcedure │    │ AccountService           │   │
                                  │  │ Catalog     │    │ (findOrCreateTelegramUser)│   │
                                  │  └─────────────┘    └──────────────────────────┘   │
                                  │       │                        │                    │
                                  │       │                        ▼                    │
                                  │       │               AccountRepository → БД       │
                                  │       │                                            │
                                  │  ProcedureSelectionState (память: выбранные        │
                                  │  процедуры по chatId)                              │
                                  └──────────────────────────────────────────────────┘
                                           │
  ┌────────────────────────────────────────┼────────────────────────────────────────┐
  │  При старте приложения                 │  Позже (выбор даты/слотов/бронирование) │
  │  configureKtormDbConnection()          │  будут вызываться:                      │
  │       │                                │  • MasterScheduleRepository             │
  │       ▼                                │  • SlotAvailabilityService              │
  │  DatabaseFactory.select()              │  • BookingService.createBooking()        │
  │  seedProceduresIfNeeded()              │  • ProcedureService.getByTypeAndSubtype │
  │       │                                └─────────────────────────────────────────┘
  │       ▼
  │  ProcedureService → ProcedureRepository → таблица procedure (8 записей)
  └──────────────────────────────────────────────────────────────────────────────────┘
```

- **Клиент** общается только с Telegram.
- **Telegram** по Bot API шлёт апдейты нашему серверу (long polling).
- **Бот** (код в `configureApplicationTgBot.kt`) обрабатывает команды и callback'и, рисует кнопки, держит состояние выбора процедур в памяти.
- **Бот ходит** в БД только через **AccountService** (при /start — найти/создать пользователя). Остальные сервисы (Procedure, Booking, SlotAvailability) пока в сценарии выбора процедур **не вызываются**; они подготовлены для шага «выбор даты → слоты → создание брони».

---

## 2. Порядок запуска приложения

В `Application.module()` вызывается:

1. `configureSerialization()` — JSON.
2. `configureRouting()` — HTTP-роуты (если есть).
3. **`configureApplicationTgBot()`** — создаёт бота и запускает long polling (бот начинает слушать Telegram).
4. **`configureKtormDbConnection()`** — подключается к БД (`DatabaseFactory.select()`) и вызывает **`seedProceduresIfNeeded()`**.

Важно: бот стартует **до** инициализации БД. Первое обращение к БД происходит при первом апдейте от пользователя (например /start), к тому моменту `configureKtormDbConnection()` уже выполнен, так что БД и сид процедур уже готовы.

---

## 3. Что написано по слоям

### 3.1 Бот (единственная «точка входа» для пользователя)

**Файл:** `src/main/kotlin/kirillale/lakinais/plugins/configureApplicationTgBot.kt`

- **Откуда вызывается:** из `Application.module()` → `configureApplicationTgBot()`.
- **Что делает:**
  - Читает токен бота (config или env `LAKI_NAILS_BOT_TOKEN`).
  - Создаёт `telegramBot(botToken)` и в корутине запускает `buildBehaviourWithLongPolling { ... }`.
  - Внутри блока вешает три обработчика:
    - **onCommand("start")** — приветствие, нижняя клавиатура с одной кнопкой, сохранение пользователя в БД через **AccountService**.
    - **onText** с фильтром по тексту «Приступить к выбору процедуры» — показывает сообщение с **inline-кнопками** (8 процедур + «🟢 ВЫБРАТЬ ДАТУ»).
    - **onDataCallbackQuery** — обрабатывает нажатия по inline-кнопкам: переключение выбора процедур (✓), обновление клавиатуры, либо переход к «выбору даты» (пока только текст «скоро»).

- **Кого вызывает:**
  - **BotProcedureCatalog** — список процедур для кнопок и индексы (p0…p7, date).
  - **ProcedureSelectionState** — in-memory состояние выбора по `chatId` (до 1 маникюра + 1 педикюра).
  - **AccountService** — только в onCommand("start"): `findOrCreateTelegramUser(...)`.
  - **Telegram Bot API** (через библиотеку): `sendMessage`, `editMessageReplyMarkup`, `answerCallbackQuery`.

- **Кого пока не вызывает:** ProcedureService, BookingService, SlotAvailabilityService, MasterScheduleRepository — они для следующих шагов (дата, слоты, запись).

---

### 3.2 Каталог процедур для бота

**Файл:** `src/main/kotlin/kirillale/lakinais/bot/BotProcedureCatalog.kt`

- **Кто использует:** только бот (`configureApplicationTgBot.kt`).
- **Что хранит:** константный список из 8 **BotProcedureOption**: текст кнопки, длительность в 15-минутных слотах (работа + перерыв), `procedureType` и `procedureSubtype` для связи с таблицей `procedure`.
- **Никуда не ходит:** это просто данные в коде. Бот по индексу (0…7) находит процедуру и обновляет состояние/клавиатуру.

Связь с БД: те же type/subtype при сиде записываются в таблицу `procedure` через ProcedureService; при создании бронирования нам понадобится уже **procedureId** из БД (например, через `ProcedureService.getByTypeAndSubtype`).

---

### 3.3 Сид процедур при старте

**Файл:** `src/main/kotlin/kirillale/lakinais/db/ProcedureSeed.kt`

- **Кто вызывает:** `configureKtormDbConnection()` после `DatabaseFactory.select()`.
- **Что делает:** для каждой процедуры из **BotProcedureCatalog** проверяет, есть ли в БД запись с такой парой (type, subtype); если нет — создаёт через **ProcedureService.createProcedure(...)** (в т.ч. `durationSlot` из каталога).
- **Цепочка:** ProcedureSeed → ProcedureService → ProcedureRepository → БД (таблица `procedure`).

Таким образом в БД всегда есть 8 процедур, совпадающих с кнопками бота по type/subtype.

---

### 3.4 Вариант А: слот = день + время (подготовка к выбору даты/слотов)

**Идея:** одна строка в `master_schedule` = один рабочий день мастера. Конкретные слоты (например по 15 минут) не хранятся в БД, а **считаются в коде** по границам дня, перерыву, блокировкам и уже занятым бронированиям.

**Что для этого сделано:**

| Компонент | Файл | Кто вызывает | Назначение |
|-----------|------|----------------|------------|
| **AvailableSlot** | `db/model/AvailableSlot.kt` | SlotAvailabilityService | Модель одного свободного слота: scheduleId, startTime, endTime, durationMinutes. |
| **BookingRepository** | `db/repositiries/BookingRepository.kt` | BookingService, SlotAvailabilityService | `findByScheduleId` → список бронирований дня; `findByScheduleIdAndStartTime` → проверка «занят ли слот». |
| **BookingService** | `db/service/BookingService.kt` | Пока только тесты / будущий сценарий бота | `createBooking` требует `startTime`; проверка конфликта через `findByScheduleIdAndStartTime`; `isSlotAvailable`, `getBookingsByScheduleId`. |
| **SlotAvailabilityService** | `db/service/SlotAvailabilityService.kt` | Будет вызываться при шаге «выбор даты» | По одной записи `master_schedule` (день) строит список слотов: шаг 15 мин, минус перерыв, блокировки мастера, уже занятые бронирования. Есть `getAvailableSlotsForDay` и `getAvailableSlotsForTwoProcedures`. |
| **MasterScheduleRepository** | `db/repositiries/MasterScheduleRepository.kt` | Будет вызываться при выборе даты | Добавлен `findOneByMasterIdAndDate` — одна запись на (мастер, дата). |

Сейчас бот **ничего из этого не дергает**. Цепочка будет такой:

- Пользователь нажал «🟢 ВЫБРАТЬ ДАТУ» → в памяти уже есть выбранные процедуры (и их `durationSlots` из каталога).
- Дальше (когда допишем шаг даты): по выбранному периоду/дате берём дни из `master_schedule` (через MasterScheduleRepository), для каждого дня вызываем **SlotAvailabilityService.getAvailableSlotsForDay** (или **getAvailableSlotsForTwoProcedures**), показываем пользователю слоты.
- После выбора слота: **BookingService.createBooking(clientId, scheduleId, procedureId, ..., startTime)**. `procedureId` можно взять из **ProcedureService.getByTypeAndSubtype** по данным из **ProcedureSelectionState** (там лежат BotProcedureOption с type/subtype).

---

## 4. Связь «API» и приложения

- **Внешнего REST API** в проекте нет. Нет контроллеров, которые бы по HTTP принимали запросы на бронирование.
- **Единственное внешнее API** — это **Telegram Bot API**: Telegram шлёт нашему серверу апдейты (сообщения, callback_query), мы отвечаем методами sendMessage, editMessageReplyMarkup и т.д. Всё это делает библиотека tgbotapi по long polling внутри `configureApplicationTgBot()`.
- **Внутри приложения** «логика» разложена так:
  - **Бот** — точка входа, только он знает про Telegram и клавиатуры.
  - **Сервисы** (Account, Procedure, Booking, SlotAvailability) — не знают про Telegram, только про домен и репозитории.
  - **Репозитории** — работают с БД через Ktorm (DatabaseFactory).

Если позже появится REST API (например Ktor routes), оно будет просто ещё одним клиентом тех же сервисов (BookingService, SlotAvailabilityService, ProcedureService и т.д.), наравне с ботом.

---

## 5. Краткая цепочка по сценариям

**Пользователь нажал /start**

1. Telegram присылает апдейт с message.text = "/start".
2. Бот: `onCommand("start")` → очистка ProcedureSelectionState для этого chatId → **AccountService.findOrCreateTelegramUser(...)** → **sendMessage**(приветствие, replyMarkup = нижняя клавиатура с одной кнопкой).

**Пользователь нажал «Приступить к выбору процедуры»**

1. Telegram присылает message с text = "Приступить к выбору процедуры".
2. Бот: `onText` (фильтр по этому тексту) → очистка выбора → **sendMessage**(текст про процедуры, replyMarkup = **buildProcedureKeyboard(emptyList())**). Клавиатура строится по **BotProcedureCatalog.all()**.

**Пользователь нажал одну из 8 процедур (inline)**

1. Telegram присылает callback_query с data = "p0" … "p7".
2. Бот: `onDataCallbackQuery` → индекс из data → **ProcedureSelectionState.toggle(chatId, option)** → **buildProcedureKeyboard(selected)** → **editMessageReplyMarkup**(то же сообщение, новая разметка) → **answerCallbackQuery**.

**Пользователь нажал «🟢 ВЫБРАТЬ ДАТУ»**

1. Telegram присылает callback_query с data = "date".
2. Бот: `onDataCallbackQuery` → **ProcedureSelectionState.getSelection(chatId)** → если пусто — answerCallbackQuery с алертом; иначе — answerCallbackQuery + **sendMessage**("Выбрано: … Выбор даты — следующий шаг (скоро)."). Сервисы слотов и бронирования пока не вызываются.

---

## 6. Где что лежит (файлы)

| Что | Где |
|-----|-----|
| Точка входа приложения | `Application.kt` → `module()` |
| Подключение БД и сид процедур | `configureKtormDbConnection.kt` |
| Вся логика бота (кнопки, обработчики) | `configureApplicationTgBot.kt` |
| Список процедур для кнопок (8 штук) | `bot/BotProcedureCatalog.kt` |
| Сид: создание записей в `procedure` | `db/ProcedureSeed.kt` |
| Модель слота (вариант А) | `db/model/AvailableSlot.kt` |
| Работа с бронированиями (БД + проверки) | `db/service/BookingService.kt`, `db/repositiries/BookingRepository.kt` |
| Расчёт свободных слотов по дню | `db/service/SlotAvailabilityService.kt` |
| Состояние выбора процедур в боте | Внутри `configureApplicationTgBot.kt`: объект `ProcedureSelectionState` |

После этого документа можно разбирать любой сценарий по шагам: кто кого вызывает и куда идут данные.
