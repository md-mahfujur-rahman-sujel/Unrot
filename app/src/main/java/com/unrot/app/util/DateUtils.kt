package com.unrot.app.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayString(): String = LocalDate.now().format(formatter)

    fun daysAgoString(days: Long): String = LocalDate.now().minusDays(days).format(formatter)

    fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** Minutes since local midnight, 0..1439. */
    fun currentMinuteOfDay(): Int {
        val now = LocalTime.now()
        return now.hour * 60 + now.minute
    }

    /** ISO day-of-week: 1 = Monday .. 7 = Sunday. */
    fun currentIsoDayOfWeek(): Int = LocalDate.now().dayOfWeek.value

    /** Formats minutes-since-midnight as a 12-hour clock label, e.g. 1320 -> "10:00 PM". */
    fun shortWeekday(isoDate: String): String {
        return runCatching {
            LocalDate.parse(isoDate, formatter).dayOfWeek.name.take(1)
        }.getOrDefault("·")
    }

    fun formatMinuteOfDay(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        val period = if (h < 12) "AM" else "PM"
        val hour12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return "%d:%02d %s".format(hour12, m, period)
    }
}
