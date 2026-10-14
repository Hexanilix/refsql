package dev.hexanilix.refsql.table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class TableColumn {

    public static class Modifiers {
        private boolean not_null = false;

        public boolean isNotNull() {
            return not_null;
        }

        public void notNull(boolean value) {
            this.not_null = value;
        }

        @Override
        public String toString() { return toString(this.not_null); }

        public String toString(boolean not_null) {
            return (not_null ? "NOT NULL" : "NULL");
        }
    }

    public enum SqlType {
        TEXT(String.class),
        INT(Integer.class),
        BIGINT(Long.class),
        BOOL(Boolean.class),
        UUID(UUID.class),
        TIMESTAMP(Timestamp.class, Instant.class);

        public static @Nullable SqlType from(@NotNull Class<?> clazz) { return from(clazz, null); }
        public static @Nullable SqlType from(@NotNull Class<?> clazz, SqlType def) {
            if (clazz.isEnum()) return SqlType.TEXT;
            if (clazz == int.class) return SqlType.INT;
            if (clazz == long.class) return SqlType.BIGINT;
            if (clazz == boolean.class) return SqlType.BOOL;

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
            if (aliases == null)
                throw new IllegalArgumentException("Aliases must contain at least one object");
            this.aliases = aliases;
        }
        SqlType(Class<?>... aliases) {
            this.sql_value = this.name();
            if (aliases == null)
                throw new IllegalArgumentException("Aliases must contain at least one object");
            this.aliases = aliases;
        }

        public String getSqlValue() {
            return sql_value;
        }

        public Class<?>[] getAliases() {
            return aliases;
        }
        public Class<?> getMainAlias() {
            return aliases[0];
        }
    }

    private String name;
    private SqlType type;

    private Modifiers modifiers;
    private Optional<String> default_value = Optional.empty();

    public TableColumn(@NotNull String name, SqlType type, Modifiers modifiers) throws IllegalArgumentException {
        if (type == null)
            throw new IllegalArgumentException("Sql type cannot be null");
        this.name = name;
        this.type = type;
        this.modifiers = modifiers != null ? modifiers : new Modifiers();
    }
    public TableColumn(String name, Class<?> type, Modifiers modifiers) {
        this(name, SqlType.from(type), modifiers);
    }
    public TableColumn(String name, SqlType type) { this(name, type, null); }

    public TableColumn(String name, Class<?> type) { this(name, type, new Modifiers()); }

    public String getName() {
        return name;
    }

    public SqlType getSqlType() {
        return type;
    }

    public Modifiers getModifiers() {
        return modifiers;
    }

    public void setDefaultValue(String sql_default) {
        this.default_value = Optional.ofNullable(sql_default);
    }

    public Optional<String> getDefaultValue() {
        return default_value;
    }

    public boolean hasDefaultValue() {
        return default_value.isPresent();
    }

    public String toColumnSql() { return toColumnSql(false); }
    public String toColumnSql(boolean is_primary_key) {
        return name + " " + type.getSqlValue() + " "
                + modifiers.toString(is_primary_key || modifiers.isNotNull())
                + default_value.map(s -> " DEFAULT " + s).orElse("");
    }

    @Override
    public String toString() {
        return "Column{" + toColumnSql() + "}";
    }
}