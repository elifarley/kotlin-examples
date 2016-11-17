package com.orgecc.persistence

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter

/* To enable autoApply:
@SpringBootApplication
@EntityScan(basePackages = { "com.orgecc.persistence", "com.orgecc.myproj.model", ... })
public class Application {
*/

@Converter(autoApply = true)
class LocalDate2Date : AttributeConverter<LocalDate, Date> {

    override fun convertToDatabaseColumn(localDate: LocalDate?): Date? {
        return Date.valueOf(localDate ?: return null)
    }

    override fun convertToEntityAttribute(sqlDate: Date?): LocalDate? = sqlDate?.toLocalDate()

}

@Converter(autoApply = true)
class LocalDateTime2Timestamp : AttributeConverter<LocalDateTime, Timestamp> {

    override fun convertToDatabaseColumn(localDateTime: LocalDateTime?): Timestamp? {
        return Timestamp.valueOf(localDateTime ?: return null)
    }

    override fun convertToEntityAttribute(sqlTimestamp: Timestamp?): LocalDateTime? =
            sqlTimestamp?.toLocalDateTime() ?: null

}
