package com.fintrace.app.data.repository

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.LocalDate
import kotlin.math.min

internal fun recurringMonthKey(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(zoneId)).toString()

internal fun recurringDayOfMonth(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(timestamp).atZone(zoneId).dayOfMonth

internal fun nextMonthlyTimestamp(
    timestamp: Long,
    anchorDayOfMonth: Int = recurringDayOfMonth(timestamp),
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val nextMonth = Instant.ofEpochMilli(timestamp).atZone(zoneId).plusMonths(1)
    return nextMonth
        .withDayOfMonth(min(anchorDayOfMonth, nextMonth.toLocalDate().lengthOfMonth()))
        .toInstant()
        .toEpochMilli()
}

internal fun timestampForRecurringMonth(
    previousTimestamp: Long,
    month: YearMonth,
    anchorDayOfMonth: Int,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val previousLocalTime = Instant.ofEpochMilli(previousTimestamp).atZone(zoneId).toLocalTime()
    val date = LocalDate.of(
        month.year,
        month.monthValue,
        min(anchorDayOfMonth, month.lengthOfMonth())
    )
    return date.atTime(previousLocalTime).atZone(zoneId).toInstant().toEpochMilli()
}
