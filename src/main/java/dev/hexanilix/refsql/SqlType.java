package dev.hexanilix.refsql;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;


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
}

