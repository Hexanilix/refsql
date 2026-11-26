package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.NotNull;

public record ColumnMapping<T extends TableColumn>(
        T column,
        ColumnReader reader,
        Class<?> clazz,
        String qualifiedName
) {
    public ColumnMapping(T column, ColumnReader reader, Class<?> clazz) {
        this(column, reader, clazz, null);
    }

    public static <T extends TableColumn> @NotNull ColumnMapping<T> of(
            @NotNull T column,
            @NotNull String tableName,
            @NotNull ColumnReader reader,
            @NotNull Class<?> clazz
    ) {
        return new ColumnMapping<>(column, reader, clazz, column.getName() + ":" + tableName);
    }
}
