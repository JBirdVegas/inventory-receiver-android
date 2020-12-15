package engineer.ima.inventory.kotlin.helpers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
import java.time.format.DateTimeFormatterBuilder

class TimeHelper {
    companion object {
        fun format(v: String): String {
            return ZonedDateTime.from(DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .optionalStart()
                    .appendOffset("+HH:MM", "Z")
                    .optionalEnd()
                    .toFormatter()
                    .parse(v))
                    .format(RFC_1123_DATE_TIME)
        }
    }
}