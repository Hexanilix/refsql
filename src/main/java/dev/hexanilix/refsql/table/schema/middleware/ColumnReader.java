package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.TypeRelationManager;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

@FunctionalInterface
public interface ColumnReader {
    Object read(ResultSet rs) throws SQLException;


    static @NotNull ColumnReader createFor(
            String name,
            Class<?> type,
            TypeRelationManager typeRelationManager
    ) {
        if (type == String.class) return rs -> rs.getString(name);

            // Primitive types
        else if (type == long.class || type == Long.class) return rs -> rs.getLong(name);
        else if (type == int.class || type == Integer.class) return rs -> rs.getInt(name);
        else if (type == short.class || type == Short.class) return rs -> rs.getShort(name);
        else if (type == byte.class || type == Byte.class) return rs -> rs.getByte(name);
        else if (type == double.class || type == Double.class) return rs -> rs.getDouble(name);
        else if (type == float.class || type == Float.class) return rs -> rs.getFloat(name);
        else if (type == boolean.class || type == Boolean.class) return rs -> rs.getBoolean(name);
        else if (type == char.class || type == Character.class)
            return rs -> {
                String value = rs.getString(name);
                return value == null ? null : value.charAt(0);
            };

            // Date and time
        else if (type == Instant.class)
            return rs -> {
                Timestamp value = rs.getTimestamp(name);
                return value == null ? null : value.toInstant();
            };
        else if (type == LocalDate.class)
            return rs -> {
                java.sql.Date value = rs.getDate(name);
                return value == null ? null : value.toLocalDate();
            };
        else if (type == LocalTime.class)
            return rs -> {
                java.sql.Time value = rs.getTime(name);
                return value == null ? null : value.toLocalTime();
            };
        else if (type == LocalDateTime.class)
            return rs -> {
                Timestamp value = rs.getTimestamp(name);
                return value == null ? null : value.toLocalDateTime();
            };
        else if (type == OffsetDateTime.class)
            return rs -> rs.getObject(name, OffsetDateTime.class);
        else if (type == ZonedDateTime.class)
            return rs -> {
                OffsetDateTime value = rs.getObject(name, OffsetDateTime.class);
                return value == null ? null : value.toZonedDateTime();
            };

            // SQL types
        else if (type == java.sql.Date.class) return rs -> rs.getDate(name);
        else if (type == java.sql.Time.class) return rs -> rs.getTime(name);
        else if (type == java.sql.Timestamp.class) return rs -> rs.getTimestamp(name);
        else if (type == java.sql.Array.class) return rs -> rs.getArray(name);

            // Other common types
        else if (type == UUID.class)
            return rs -> {
                Object value = rs.getObject(name);
                if (value == null) return null;
                return value instanceof UUID ? value : UUID.fromString(value.toString());
            };
        else if (type == URI.class)
            return rs -> {
                String value = rs.getString(name);
                return value == null ? null : URI.create(value);
            };
        else if (type == URL.class)
            return rs -> {
                String value = rs.getString(name);
                try {
                    return value == null ? null : URI.create(value).toURL();
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid url provided in column " + name, e);
                }
            };
        else if (type == BigInteger.class)
            return rs -> {
                BigDecimal value = rs.getBigDecimal(name);
                return value == null ? null : value.toBigIntegerExact();
            };
        else if (type == BigDecimal.class)
            return rs -> rs.getBigDecimal(name);

            // Relations
        else if (typeRelationManager != null) {
            ClassSqlRelationDefinition<?> def = typeRelationManager.get(type);
            if (def != null)
                return rs -> def.fromSql(rs, name);
        }

        // Enums
        if (type.isEnum()) {
            Class<? extends Enum> enumType = type.asSubclass(Enum.class);
            return rs -> {
                String value = rs.getString(name);
                return value == null ? null : Enum.valueOf(enumType, value);
            };
        }

        return rs -> rs.getObject(name, type);
    }

}