package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.TypeRelationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;

@FunctionalInterface
public interface ColumnReader {

    Object read(ResultSet rs, ResultSetColumnMetadata meta) throws SQLException;

    static @NotNull ColumnReader createFor(
            String name,
            Class<?> type,
            TypeRelationManager typeRelationManager
    ) {
        if (type == String.class) return (rs, m) -> rs.getString(m.index());

        // Primitive types
        if (type == long.class || type == Long.class) return (rs, m) -> getNullableLong(rs, m.index());
        if (type == int.class || type == Integer.class) return (rs, m) -> getNullableInt(rs, m.index());
        if (type == short.class || type == Short.class) return (rs, m) -> getNullableShort(rs, m.index());
        if (type == byte.class || type == Byte.class) return (rs, m) -> getNullableByte(rs, m.index());
        if (type == double.class || type == Double.class) return (rs, m) -> getNullableDouble(rs, m.index());
        if (type == float.class || type == Float.class) return (rs, m) -> getNullableFloat(rs, m.index());
        if (type == boolean.class || type == Boolean.class) return (rs, m) -> getNullableBoolean(rs, m.index());

        if (type == char.class || type == Character.class) {
            return (rs, m) -> {
                String value = rs.getString(m.index());;
                return value == null ? null : value.charAt(0);
            };
        }

        // Date and time
        if (type == Instant.class) {
            return (rs, m) -> {
                Timestamp value = rs.getTimestamp(m.index());;
                return value == null ? null : value.toInstant();
            };
        }

        if (type == LocalDate.class) {
            return (rs, m) -> {
                java.sql.Date value = rs.getDate(m.index());;
                return value == null ? null : value.toLocalDate();
            };
        }

        if (type == LocalTime.class) {
            return (rs, m) -> {
                Time value = rs.getTime(m.index());;
                return value == null ? null : value.toLocalTime();
            };
        }

        if (type == LocalDateTime.class) {
            return (rs, m) -> {
                Timestamp value = rs.getTimestamp(m.index());;
                return value == null ? null : value.toLocalDateTime();
            };
        }

        if (type == OffsetDateTime.class) return (rs, m) -> rs.getObject(name, OffsetDateTime.class);

        if (type == ZonedDateTime.class) {
            return (rs, m) -> {
                OffsetDateTime value = rs.getObject(name, OffsetDateTime.class);
                return value == null ? null : value.toZonedDateTime();
            };
        }

        // SQL types
        if (type == java.sql.Date.class) return (rs, m) -> rs.getDate(m.index());;
        if (type == Time.class) return (rs, m) -> rs.getTime(m.index());;
        if (type == Timestamp.class) return (rs, m) -> rs.getTimestamp(m.index());;
        if (type == java.sql.Array.class) return (rs, m) -> rs.getArray(m.index());;

        // Other common types
        if (type == UUID.class) {
            return (rs, m) -> {
                Object value = rs.getObject(m.index());;
                if (value == null) return null;
                return value instanceof UUID ? value : UUID.fromString(value.toString());
            };
        }

        if (type == URI.class) {
            return (rs, m) -> {
                String value = rs.getString(m.index());;
                return value == null ? null : URI.create(value);
            };
        }

        if (type == URL.class) {
            return (rs, m) -> {
                String value = rs.getString(m.index());;
                if (value == null) return null;

                try {
                    return URI.create(value).toURL();
                } catch (MalformedURLException e) {
                    throw new SQLException("Invalid URL in column " + name, e);
                }
            };
        }

        if (type == BigInteger.class) {
            return (rs, m) -> {
                BigDecimal value = rs.getBigDecimal(m.index());;
                return value == null ? null : value.toBigIntegerExact();
            };
        }

        if (type == BigDecimal.class) return (rs, m) -> rs.getBigDecimal(m.index());;

        // Relations
        if (typeRelationManager != null) {
            ClassSqlRelationDefinition<?> def = typeRelationManager.get(type);
            if (def != null) return def::fromSql;
        }

        // Enums
        if (type.isEnum()) {
            Class<? extends Enum> enumType = type.asSubclass(Enum.class);

            return (rs, m) -> {
                String value = rs.getString(m.index());
                return value == null ? null : Enum.valueOf(enumType, value);
            };
        }

        return (rs, m) -> rs.getObject(name, type);
    }

    // Nullable primitive readers
    private static @Nullable @Unmodifiable Object getNullableLong(@NotNull ResultSet rs, int index) throws SQLException {
        long value = rs.getLong(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableInt(@NotNull ResultSet rs, int index) throws SQLException {
        int value = rs.getInt(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableShort(@NotNull ResultSet rs, int index) throws SQLException {
        short value = rs.getShort(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableByte(@NotNull ResultSet rs, int index) throws SQLException {
        byte value = rs.getByte(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableDouble(@NotNull ResultSet rs, int index) throws SQLException {
        double value = rs.getDouble(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableFloat(@NotNull ResultSet rs, int index) throws SQLException {
        float value = rs.getFloat(index);
        return rs.wasNull() ? null : value;
    }

    private static @Nullable @Unmodifiable Object getNullableBoolean(@NotNull ResultSet rs, int index) throws SQLException {
        boolean value = rs.getBoolean(index);
        return rs.wasNull() ? null : value;
    }
}