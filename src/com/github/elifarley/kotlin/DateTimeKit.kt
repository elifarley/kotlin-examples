import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

/**
 * Created by elifarley on 31/08/16.
 */

object DateTimeKit {
    @JvmOverloads
    fun TemporalAccessor.toDate(zoneOffset: ZoneOffset = ZoneOffset.UTC) = LocalDateTime.from(this).toDate(zoneOffset)
    @JvmOverloads
    fun LocalDate.toDate(zoneOffset: ZoneOffset = ZoneOffset.UTC) = this.atStartOfDay().toDate(zoneOffset)
    @JvmOverloads
    fun LocalDateTime.toDate(zoneOffset: ZoneOffset = ZoneOffset.UTC) = Date.from(this.toInstant(zoneOffset))
    val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneOffset.UTC)!!
    fun DateTimeFormatter.format(date: Date) = this.format(date.toInstant())!!
    fun Instant.toGregorianCalendar() =
        GregorianCalendar.from(ZonedDateTime.ofInstant(this, ZoneOffset.UTC))
}
