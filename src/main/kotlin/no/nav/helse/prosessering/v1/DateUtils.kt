package no.nav.helse.prosessering.v1

import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.streams.toList

object DateUtils {
    fun antallVirkedager(fraOgMed: LocalDate, tilOgMed: LocalDate): Int {
        return fraOgMed.datesUntil(tilOgMed.plusDays(1)).toList()
            .filter { it.dayOfWeek != SATURDAY && it.dayOfWeek != SUNDAY }
            .size
    }
}

internal fun ZonedDateTime.norskDag() = when(dayOfWeek) {
    MONDAY -> "Mandag"
    TUESDAY -> "Tirsdag"
    WEDNESDAY -> "Onsdag"
    THURSDAY -> "Torsdag"
    FRIDAY -> "Fredag"
    SATURDAY -> "Lørdag"
    else -> "Søndag"
}