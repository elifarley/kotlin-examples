package com.orgecc.io

/**
 * Created by elifarley on 02/09/16.
 */

import com.orgecc.DateTimeKit.format
import org.beanio.types.ConfigurableTypeHandler
import org.beanio.types.LocaleSupport
import org.beanio.types.TypeConversionException
import java.text.ParseException
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class DateTypeHandler(patternP: String? = null) :
        LocaleSupport(), ConfigurableTypeHandler, Cloneable {

    var timeZone: TimeZone = TimeZone.getDefault()

    var format: DateTimeFormatter? = null
        get() {
            if (field == null)
                field = createDateFormat()
            return field
        }
        set(v) {
            if (v != null) throw IllegalArgumentException(v.toString())
        }

    val dtFormatter: DateTimeFormatter get() = format!!

    var pattern: String? = DEFAULT_PATTERN
        set(v) {
            // validate the pattern
            try {
                if (v != null) {
                    DateTimeFormatter.ofPattern(v)
                }
            } catch (ex: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid date format pattern '$v': ${ex.message}")
            }

            field = v
            format = null
        }

    init {
        pattern = patternP
    }

    companion object {
        private const val DEFAULT_PATTERN = "yyyyMMddHHmmss"
        private const val TIMEZONE_SETTING = "timezone"
    }

    @Throws(TypeConversionException::class)
    override fun parse(text: String?) =
            if (text.isNullOrBlank()) {
                null

            } else try {
                dtFormatter.parse(text)

            } catch (ex: ParseException) {
                throw TypeConversionException(ex)

            }

    override fun format(value: Any?) = if (value == null) ""
    else dtFormatter.format(value as Date)

    override fun getType(): Class<*> = Date::class.java

    @Throws(IllegalArgumentException::class)
    override fun newInstance(properties: Properties): DateTypeHandler {
        val userPattern = properties.getProperty(ConfigurableTypeHandler.FORMAT_SETTING)
        val tz = properties.getProperty(TIMEZONE_SETTING)

        if (userPattern == null || userPattern.length == 0) {
            pattern = userPattern
            return@newInstance this
        }

        if (userPattern == pattern) {
            return@newInstance this
        }

        try {
            val result = this.clone() as DateTypeHandler

            result.locale = locale
            result.timeZone = if (tz.isNullOrBlank()) TimeZone.getDefault() else TimeZone.getTimeZone(tz)
            result.pattern = userPattern
            return@newInstance result

        } catch (e: CloneNotSupportedException) {
            throw IllegalStateException(e)
        }
    }

    /**
     * Creates a default date format when no pattern is set.
     * @return the default date format
     */
    private fun createDefaultDateFormat() =
            DateTimeFormatter.ofPattern(DEFAULT_PATTERN).withZone(ZoneOffset.systemDefault())

    /**
     * Creates the DateFormat to use to parse and format the field value.
     * @return the DateFormat for type conversion
     */
    private fun createDateFormat() = if (pattern == null) {
        createDefaultDateFormat()

    } else DateTimeFormatter.ofPattern(pattern, locale).let {
        it.withZone(timeZone.toZoneId())

    }

}
