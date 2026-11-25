package dev.hexanilix.refsql.table;

import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class TableIndex {

    private final TableColumn[] columns;
    private final String extra;

    public TableIndex(TableColumn[] columns, String extra) {
        this.columns = Objects.requireNonNull(columns);
        this.extra = extra;
    }

    public TableIndex(TableColumn... columns) {
        this(columns, null);
    }

    public TableColumn[] getColumns() {
        return columns;
    }

    public @Nullable String getExtra() {
        return extra;
    }

    public boolean duplicatesPrimaryKey(
            @Nullable List<? extends TableColumn> primaryKeys) {

        if (primaryKeys == null || columns.length != primaryKeys.size())
            return false;

        for (int i = 0; i < columns.length; i++) {
            if (!columns[i].equals(primaryKeys.get(i)))
                return false;
        }

        return true;
    }

    @Contract("_, _, _ -> param1")
    private static StringBuilder appendJoin(
            StringBuilder sb,
            String separator,
            TableColumn @NotNull[] columns) {

        boolean addSeparator = false;

        for (TableColumn column : columns) {
            if (addSeparator) sb.append(separator);
            sb.append(column.getName());
            addSeparator = true;
        }

        return sb;
    }

    public @NotNull String buildSql(
            @NotNull BaseTableSchema<?> schema) {

        StringBuilder sb = new StringBuilder("CREATE INDEX IF NOT EXISTS idx_")
                .append(schema.getName())
                .append('_');

        appendJoin(sb, "_", columns)
                .append(" ON ")
                .append(schema.getName())
                .append(" (");

        appendJoin(sb, ", ", columns);

        if (extra != null && !extra.isBlank())
            sb.append(' ').append(extra);

        return sb.append(')').toString();
    }
}