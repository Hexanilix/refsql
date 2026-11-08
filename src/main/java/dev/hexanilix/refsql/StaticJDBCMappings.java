package dev.hexanilix.refsql;

import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

public final class StaticJDBCMappings {

    /**
     * Converts classes which are generally valid SQL types, into direct JDBC SQL compatible types
     * @param obj Instance to convert
     * @return A valid JDBC SQL type
     */
    public static @Nullable Object internalMappingToSql(Object obj) {
        return switch (obj) {
            case null -> null;
            case Instant instant -> Timestamp.from(instant);
            case LocalDate date -> java.sql.Date.valueOf(date);
            case LocalTime time -> Time.valueOf(time);
            case LocalDateTime dateTime -> Timestamp.valueOf(dateTime);
            case Enum<?> e -> e.name();
            case Character c -> c.toString();
            case BigInteger integer -> new BigDecimal(integer);
            case Optional<?> optional -> optional.orElse(null);
            case URI uri -> uri.toString();
            case URL url -> url.toString();
            default -> obj;
        };
    }

}
