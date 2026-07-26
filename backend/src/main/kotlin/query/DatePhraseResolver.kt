package query

import java.sql.Connection
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun getReferenceDate(connection: Connection): LocalDate {
    val sql = "SELECT MAX(sale_date) AS ref_date FROM sales"
    connection.createStatement().use { statement ->
        val resultSet = statement.executeQuery(sql)
        resultSet.next()
        return resultSet.getDate("ref_date").toLocalDate()
    }
}

data class DateRange(val start: LocalDate, val end: LocalDate)

fun resolveDatePhrase(queryText: String, refDate: LocalDate): DateRange? {
    val text = queryText.lowercase()

    return when {
        text.contains("this weekend") -> {
            val saturday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            val sunday = saturday.plusDays(1)
            DateRange(saturday, sunday)
        }
        text.contains("last weekend") -> {
            val mostRecentSaturday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
            val mostRecentSunday = mostRecentSaturday.plusDays(1)
            val saturday = if (mostRecentSunday.isBefore(refDate)) mostRecentSaturday else mostRecentSaturday.minusWeeks(1)
            DateRange(saturday, saturday.plusDays(1))
        }
        text.contains("yesterday")    -> DateRange(refDate.minusDays(1), refDate.minusDays(1))
        text.contains("this week")    -> {
            val startOfWeek = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            DateRange(startOfWeek, refDate)
        }
        text.contains("last week")    -> {
            val startOfThisWeek = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val startOfLastWeek = startOfThisWeek.minusWeeks(1)
            val endOfLastWeek = startOfThisWeek.minusDays(1)
            DateRange(startOfLastWeek, endOfLastWeek)
        }
        text.contains("this month")   -> {
            val startOfMonth = refDate.withDayOfMonth(1)
            DateRange(startOfMonth, refDate)
        }
        text.contains("last month")   -> {
            val startOfThisMonth = refDate.withDayOfMonth(1)
            val startOfLastMonth = startOfThisMonth.minusMonths(1)
            val endOfLastMonth = startOfThisMonth.minusDays(1)
            DateRange(startOfLastMonth, endOfLastMonth)
        }
        text.contains("today")        -> DateRange(refDate, refDate)
        else -> null
    }
}