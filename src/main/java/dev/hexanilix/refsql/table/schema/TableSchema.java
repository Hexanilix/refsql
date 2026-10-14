package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.TableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class TableSchema extends BaseTableSchema<TableColumn> {

    public TableSchema(@NotNull String name, @Nullable TypeRelationManager type_relation_manager, @NotNull List<TableColumn> columns, @Nullable List<TableColumn> primary_keys) {
        super(name, type_relation_manager, columns, primary_keys);
    }

    public TableSchema(@NotNull String name, TypeRelationManager type_relation_manager, @NotNull List<TableColumn> columns) {
        super(name, type_relation_manager, columns);
    }

    public TableSchema(@NotNull String name, @NotNull List<TableColumn> columns) {
        super(name, columns);
    }

}
