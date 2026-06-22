package kirillale.lakinais.booking

enum class BookingSlotMode {
    /** Две процедуры в один непрерывный слот. */
    CONSECUTIVE,
    /** Процедуры записываются в разное время (сначала приоритетная). */
    SPLIT,
}
