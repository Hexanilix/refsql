package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.TypeRelationManager;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.*;
import java.time.*;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@FunctionalInterface
public interface ColumnReader {

    Object read(ResultSet rs) throws SQLException;

    static @NotNull Set<String> getColumns(@NotNull ResultSet rs) throws SQLException {
        Set<String> columnNames = new HashSet<>();
        ResultSetMetaData metaData = rs.getMetaData();

        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columnNames.add(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }

        return columnNames;
    }

    static @NotNull ColumnReader createFor(
            String name,
            Class<?> type,
            TypeRelationManager typeRelationManager
    ) {
        if (type == String.class) return rs -> rs.getString(name);

        // Primitive types
        if (type == long.class || type == Long.class) return rs -> getNullableLong(rs, name);
        if (type == int.class || type == Integer.class) return rs -> getNullableInt(rs, name);
        if (type == short.class || type == Short.class) return rs -> getNullableShort(rs, name);
        if (type == byte.class || type == Byte.class) return rs -> getNullableByte(rs, name);
        if (type == double.class || type == Double.class) return rs -> getNullableDouble(rs, name);
        if (type == float.class || type == Float.class) return rs -> getNullableFloat(rs, name);
        if (type == boolean.class || type == Boolean.class) return rs -> getNullableBoolean(rs, name);

        if (type == char.class || type == Character.class) {
            return rs -> {
                String value = rs.getString(name);
                return value == null ? null : value.charAt(0);
            };
        }

        // Date and time
        if (type == Instant.class) {
            return rs -> {
                Timestamp value = rs.getTimestamp(name);
                return value == null ? null : value.toInstant();
            };
        }

        if (type == LocalDate.class) {
            return rs -> {
                java.sql.Date value = rs.getDate(name);
                return value == null ? null : value.toLocalDate();
            };
        }

        if (type == LocalTime.class) {
            return rs -> {
                Time value = rs.getTime(name);
                return value == null ? null : value.toLocalTime();
            };
        }

        if (type == LocalDateTime.class) {
            return rs -> {
                Timestamp value = rs.getTimestamp(name);
                return value == null ? null : value.toLocalDateTime();
            };
        }

        if (type == OffsetDateTime.class) return rs -> rs.getObject(name, OffsetDateTime.class);

        if (type == ZonedDateTime.class) {
            return rs -> {
                OffsetDateTime value = rs.getObject(name, OffsetDateTime.class);
                return value == null ? null : value.toZonedDateTime();
            };
        }

        // SQL types
        if (type == java.sql.Date.class) return rs -> rs.getDate(name);
        if (type == Time.class) return rs -> rs.getTime(name);
        if (type == Timestamp.class) return rs -> rs.getTimestamp(name);
        if (type == java.sql.Array.class) return rs -> rs.getArray(name);

        // Other common types
        if (type == UUID.class) {
            return rs -> {
                Object value = rs.getObject(name);
                if (value == null) return null;
                return value instanceof UUID ? value : UUID.fromString(value.toString());
            };
        }

        if (type == URI.class) {
            return rs -> {
                String value = rs.getString(name);
                return value == null ? null : URI.create(value);
            };
        }

        if (type == URL.class) {
            return rs -> {
                String value = rs.getString(name);
                if (value == null) return null;

                try {
                    return URI.create(value).toURL();
                } catch (MalformedURLException e) {
                    throw new SQLException("Invalid URL in column " + name, e);
                }
            };
        }

        if (type == BigInteger.class) {
            return rs -> {
                BigDecimal value = rs.getBigDecimal(name);
                return value == null ? null : value.toBigIntegerExact();
            };
        }

        if (type == BigDecimal.class) return rs -> rs.getBigDecimal(name);

        // Relations
        if (typeRelationManager != null) {
            ClassSqlRelationDefinition<?> def = typeRelationManager.get(type);
            if (def != null) return rs -> def.fromSql(rs, name);
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

    // Nullable primitive readers

    private static Object getNullableLong(ResultSet rs, String name) throws SQLException {
        long value = rs.getLong(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableInt(ResultSet rs, String name) throws SQLException {
        int value = rs.getInt(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableShort(ResultSet rs, String name) throws SQLException {
        short value = rs.getShort(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableByte(ResultSet rs, String name) throws SQLException {
        byte value = rs.getByte(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableDouble(ResultSet rs, String name) throws SQLException {
        double value = rs.getDouble(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableFloat(ResultSet rs, String name) throws SQLException {
        float value = rs.getFloat(name);
        return rs.wasNull() ? null : value;
    }

    private static Object getNullableBoolean(ResultSet rs, String name) throws SQLException {
        boolean value = rs.getBoolean(name);
        return rs.wasNull() ? null : value;
    }
}