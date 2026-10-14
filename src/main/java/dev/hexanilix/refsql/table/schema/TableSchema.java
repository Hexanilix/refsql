package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.table.TableColumn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TableSchema extends BaseTableSchema<TableColumn> {

    public TableSchema(String name, Map<String, TableColumn> columns, TableColumn primaryKey) {
        super(name, columns, primaryKey, null);
    }

    public TableSchema(String name) {
        super(name);
    }

    public TableSchema(String name, TableColumn @NotNull ... columns) {
        super(name, columns);
    }

}
