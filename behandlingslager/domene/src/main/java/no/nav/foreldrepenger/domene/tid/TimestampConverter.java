package no.nav.foreldrepenger.domene.tid;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.TimeZone;

public class TimestampConverter {

    private TimestampConverter() {
    }

    public static LocalDate toLocalDate(Object sqlTimestamp) {
        return switch (sqlTimestamp) {
            case null -> null;
            case java.sql.Timestamp sqlTs -> LocalDateTime.ofInstant(sqlTs.toInstant(), TimeZone.getDefault().toZoneId()).toLocalDate();
            case java.sql.Date sqlDate -> sqlDate.toLocalDate();
            case java.time.LocalDateTime localDateTime -> localDateTime.toLocalDate();
            case java.time.LocalDate localDate -> localDate;
            case java.time.OffsetDateTime offsetDateTime -> offsetDateTime.toLocalDate();
            case java.time.ZonedDateTime zonedDateTime -> zonedDateTime.toLocalDate();
            case java.util.Date utilDate -> utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            default -> throw new IllegalArgumentException("Unsupported SQL timestamp: " + sqlTimestamp.getClass().getSimpleName());
        };
    }

    public static LocalDateTime toLocalDateTime(Object sqlTimestamp) {
        return switch (sqlTimestamp) {
            case null -> null;
            case java.sql.Timestamp sqlTs -> LocalDateTime.ofInstant(sqlTs.toInstant(), TimeZone.getDefault().toZoneId());
            case java.sql.Date sqlDate -> sqlDate.toLocalDate().atStartOfDay();
            case java.time.LocalDateTime localDateTime -> localDateTime;
            case java.time.LocalDate localDate -> localDate.atStartOfDay();
            case java.time.OffsetDateTime offsetDateTime -> offsetDateTime.toLocalDateTime();
            case java.time.ZonedDateTime zonedDateTime -> zonedDateTime.toLocalDateTime();
            case java.util.Date utilDate -> utilDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            default -> throw new IllegalArgumentException("Unsupported SQL timestamp: " + sqlTimestamp.getClass().getSimpleName());
        };
    }

}
