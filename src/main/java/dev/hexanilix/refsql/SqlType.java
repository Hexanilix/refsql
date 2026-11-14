package dev.hexanilix.refsql;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import java.util.regex.Pattern;

import static dev.hexanilix.refsql.StaticJDBCMappings.internalMappingToSql;


public enum SqlType {
    TEXT(String.class),
    CHAR("CHAR(1)", Character.class, char.class),

    INT("INTEGER", Integer.class, int.class),
    BIGINT(Long.class, long.class),
    SMALLINT(Short.class, short.class),
    TINYINT("SMALLINT", Byte.class, byte.class),

    BOOL("BOOLEAN", Boolean.class, boolean.class),

    REAL(Float.class, float.class),
    DOUBLE("DOUBLE PRECISION", Double.class, double.class),
    NUMERIC(BigDecimal.class, BigInteger.class),

    UUID(UUID.class),

    DATE(LocalDate.class, java.sql.Date.class),
    TIME(LocalTime.class, java.sql.Time.class),
    TIMESTAMP(Timestamp.class, LocalDateTime.class),
    TIMESTAMPTZ(Instant.class, OffsetDateTime.class, ZonedDateTime.class),

    BYTEA(byte[].class);

    public static @Nullable SqlType from(@NotNull Class<?> clazz) {
        return from(clazz, null);
    }

    public static @Nullable SqlType from(
            @NotNull Class<?> clazz,
            @Nullable SqlType def
    ) {
        if (clazz.isEnum())
            return SqlType.TEXT;

        for (SqlType sqlType : SqlType.values())
            for (Class<?> alias : sqlType.aliases)
                if (alias.equals(clazz))
                    return sqlType;

        return def;
    }

    private final String sql_value;
    private final Class<?>[] aliases;

    SqlType(String sql_value, Class<?>... aliases) {
        this.sql_value = sql_value;
        this.aliases = aliases;
    }

    SqlType(Class<?>... aliases) {
        this.sql_value = this.name();
        this.aliases = aliases;
    }

    public String getSqlValue() {
        return sql_value;
    }

    public Class<?>[] getAliases() {
        return aliases.clone();
    }

    public Class<?> getMainAlias() {
        return aliases[0];
    }


    /**
     * Converts a class fields value into a compatible SQL type
     *
     * @param value The object to convert into a valid SQL type]
     * @return A JDBC Potsrges compatible object value
     * @throws IllegalStateException Occurs when the resulting object is not a SQL compatible value
     */
    public static Object convert(Object value) throws IllegalStateException { return convert(value, null); }

    /**
     * Converts a class fields value into a compatible SQL type
     *
     * @param value The object to convert into a valid SQL type
     * @param mgr A type relation manager
     * @return A JDBC Potsrges compatible object value
     * @param <Obj> Helper type
     * @throws IllegalStateException Occurs when the resulting object is not a SQL compatible value
     */
    public static <Obj> Object convert(Object value, @Nullable TypeRelationManager mgr) throws IllegalStateException {
        if (value != null) {
            Class<Obj> value_clazz = (Class<Obj>) value.getClass();
            boolean to_convert = true;
            if (mgr != null) {
                ClassSqlRelationDefinition<Obj> def = mgr.get(value_clazz);
                if (def != null) {
                    value = def.convertToSql((Obj) value);
                    to_convert = false;
                }
            }
            if (to_convert) {
                if (value.getClass().isEnum()) {
                    value = ((Enum<?>) value).name();
                    to_convert = false;
                }
            }

            // This mapping is always run since it's reasonable to assume that, for example, an Instant or an Enum
            // returned from a conversion is a valid SQL value, but JDBC won't accept it normally, thus the extra conversion
            value = internalMappingToSql(value);
        }

        if (value != null && SqlType.from(value.getClass()) == null)
            throw new IllegalStateException(
                    "Value " + value + " is not a valid Sql type. Got a " + value.getClass()
            );

        return value;
    }

    // I audibly exclaimed 'BOMBOCLAT' when I saw ChatGPT spit this atrocity out
    private static final Pattern PG_NUMBER = Pattern.compile(
            "[+-]?(?:[0-9]+(?:_[0-9]+)*(?:\\.[0-9]+(?:_[0-9]+)*)?|\\.[0-9]+(?:_[0-9]+)*|[0-9]+(?:_[0-9]+)*\\.)(?:[eE][+-]?[0-9]+(?:_[0-9]+)*)?"
    );

    public static boolean isValidPostgresNumber(String s) {
        return s != null && PG_NUMBER.matcher(s).matches();
    }

    @Contract(pure = true)
    public static @NotNull String wrapRawValueForQuery(@NotNull String s) {
        if (isValidPostgresNumber(s)) {
            return s;
        } else if ("null".equalsIgnoreCase(s)) {
            return "NULL";
        }

        return "'" + s.replace("'", "''") + "'";
    }

}

