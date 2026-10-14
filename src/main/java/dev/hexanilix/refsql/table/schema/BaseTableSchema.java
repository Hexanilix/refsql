package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.TableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BaseTableSchema<CT extends TableColumn> {

    protected final String name;
    protected final Map<String, CT> columns;
    protected CT primary_key;
    private final TypeRelationManager type_relation_manager;
    //    protected final Map<String, Constraint> columns;
    public BaseTableSchema(String name, Map<String, CT> columns, CT primary_key, TypeRelationManager type_relation_manager) {
        this.name = name;
        this.columns = columns;
        if (primary_key != null && !columns.containsValue(primary_key))
            throw new IllegalArgumentException("Primary key column must exist among provided columns");
        this.primary_key = primary_key;
        this.type_relation_manager = type_relation_manager;
    }
    public BaseTableSchema(String name) {
        this(name, new HashMap<>(), null, null);
    }
    public BaseTableSchema(String name, TypeRelationManager type_relation_manager) {
        this(name, new HashMap<>(), null, type_relation_manager);
    }

    public BaseTableSchema(String name, CT @NotNull ... columns) {
        this(name);
        for (CT column : columns)
            this.columns.put(column.getName(), column);
    }

    public String getName() {
        return name;
    }

    public Map<String, CT> getColumnMap() {
        return columns;
    }

    public Collection<CT> getColumns() {
        return columns.values();
    }

    public TableColumn getColumn(String name) {
        return columns.get(name);
    }

    public boolean hasPrimaryKey() {
        return primary_key != null;
    }

    public @Nullable CT getPrimaryKey() {
        return primary_key;
    }

    public boolean hasTypeRelationManager() {
        return type_relation_manager != null;
    }
    public TypeRelationManager getTypeRelationManager() {
        return type_relation_manager;
    }

    public String createTableIfNotExistsSql() {
        return "CREATE TABLE IF NOT EXISTS " + name + " (" + buildTableBody() + ");";
    }

    public String buildTableBody() {
        StringBuilder sb = new StringBuilder();

        boolean comma = false;
        for (Map.Entry<String, CT> entry : columns.entrySet()) {
            TableColumn column = entry.getValue();

            if (comma) sb.append(", ");
            sb.append(column.toColumnSql(column == primary_key));
            comma = true;
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "TableSchema{" +
                "name='" + name + '\'' +
                ", columns=" + columns +
                '}';
    }


}
