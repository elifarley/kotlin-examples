package com.orgecc.persistence

import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class LocalDate2Date : AttributeConverter<LocalDate, Date> {

    override fun convertToDatabaseColumn(localDateTime: LocalDate?): Date? {
        return Date.valueOf(localDateTime ?: return null)
    }

    override fun convertToEntityAttribute(sqlTimestamp: Date?): LocalDate? = sqlTimestamp?.toLocalDate()

}

@Converter(autoApply = true)
class LocalDateTime2Timestamp : AttributeConverter<LocalDateTime, Timestamp> {

    override fun convertToDatabaseColumn(localDateTime: LocalDateTime?): Timestamp? {
        return Timestamp.valueOf(localDateTime ?: return null)
    }

    override fun convertToEntityAttribute(sqlTimestamp: Timestamp?): LocalDateTime? =
            sqlTimestamp?.toLocalDateTime() ?: null

}
