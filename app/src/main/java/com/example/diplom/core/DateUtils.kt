package com.example.diplom.core

import java.time.DayOfWeek
import java.time.LocalDate

object DateUtils {
    fun todayIso(): String = LocalDate.now().toString()

    fun isoWeekStart(date: LocalDate = LocalDate.now()): String {
        val monday = date.with(DayOfWeek.MONDAY)
        return monday.toString()
    }
}
