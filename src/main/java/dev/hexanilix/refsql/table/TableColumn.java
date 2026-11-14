package dev.hexanilix.refsql.table;

import dev.hexanilix.refsql.SqlType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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

    private String name;
    private SqlType type;

    private Modifiers modifiers;
    private @NotNull Optional<String> default_value = Optional.empty();

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
                + default_value.map(s -> " DEFAULT " + SqlType.wrapRawValueForQuery(s)).orElse("");
    }

    @Override
    public String toString() {
        return "Column{" + toColumnSql() + "}";
    }
}