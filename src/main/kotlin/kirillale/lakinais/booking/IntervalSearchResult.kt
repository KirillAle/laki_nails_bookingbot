package kirillale.lakinais.booking

sealed class IntervalSearchResult {
    data class Intervals(val intervals: List<DateInterval>) : IntervalSearchResult()
    data object ConsecutiveUnavailable : IntervalSearchResult()
    data object NothingAvailable : IntervalSearchResult()
}
