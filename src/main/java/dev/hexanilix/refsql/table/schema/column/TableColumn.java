package dev.hexanilix.refsql.table.schema.column;

import dev.hexanilix.refsql.SqlType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TableColumn {

    private final String name;
    private final SqlType type;
    private final boolean not_null;
    private final boolean unique;
    private final @Nullable ColumnDefault<?> default_value;

    public TableColumn(@NotNull String name, SqlType type, boolean not_null, boolean unique, @Nullable ColumnDefault<?> default_value) throws IllegalArgumentException {
        if (type == null)
            throw new IllegalArgumentException("Sql type cannot be null");
        this.name = name;
        this.type = type;
        this.not_null = not_null;
        this.unique = unique;
        this.default_value = default_value;
    }
    public TableColumn(String name, Class<?> type, boolean not_null, boolean unique) throws IllegalArgumentException {
        this(name, SqlType.from(type), not_null, unique,  null);
    }
    public TableColumn(String name, SqlType type) { this(name, type, false, false, null); }


    public String getName() {
        return name;
    }

    public SqlType getSqlType() {
        return type;
    }

    public boolean isNotNull() {
        return not_null;
    }

    public boolean isUnique() {
        return unique;
    }

    public ColumnDefault<?> getDefaultValue() {
        return default_value;
    }

    public boolean hasDefaultValue() {
        return default_value != null;
    }

    public String toColumnSql() { return toColumnSql(false); }
    public String toColumnSql(boolean is_primary_key) {
        return name + " " + type.getSqlValue()
                + (not_null || is_primary_key ? " NOT NULL" : " NULL")
                + (default_value != null ? " DEFAULT " + default_value.getAsSqlDefaultString() : "");
    }

    @Override
    public String toString() {
        return "Column{" + toColumnSql() + "}";
    }
}