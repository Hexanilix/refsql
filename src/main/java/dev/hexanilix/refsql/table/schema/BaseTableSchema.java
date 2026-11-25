package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.TableIndex;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class BaseTableSchema<TC extends TableColumn> {

    public static final class Builder<T extends TableColumn> {

        private final List<T> columns = new ArrayList<>();
        private final List<T> primary_keys = new ArrayList<>();
        private final String name;
        private TypeRelationManager type_relation_manager;
        public Builder(String name) {
            this.name = name;
        }

        public void column(T... columns) {
            this.columns.addAll(Arrays.asList(columns));
        }

        public void primaryKeys(T... primary_keys) {
            this.primary_keys.addAll(Arrays.asList(primary_keys));
        }

        public void typeRelationManager(TypeRelationManager type_relation_manager) {
            this.type_relation_manager = type_relation_manager;
        }

        @Contract(" -> new")
        public @NotNull BaseTableSchema<T> build() {
            return new BaseTableSchema<>(name, type_relation_manager, columns, primary_keys, null);
        }
    }

    protected final String name;
    protected final @Unmodifiable @NotNull List<TC> columns;
    protected final @Unmodifiable @Nullable List<TC> primary_keys;
    protected final @Unmodifiable @Nullable List<TableIndex> indexes;
//    protected final @Unmodifiable @Nullable List<ForeignKeyRelation> foreign_keys;
//    protected final @Nullable BaseTableRepository<BaseTableSchema<TC>> repo;
    private final TypeRelationManager type_relation_manager;
    public BaseTableSchema(
            @NotNull String name,
            @Nullable TypeRelationManager type_relation_manager,
            @NotNull List<TC> columns,
            @Nullable List<TC> primary_keys,
            @Nullable List<TableIndex> indexes
    ) {
        this.name = name;
        this.type_relation_manager = type_relation_manager;

        List<TC> temp = new ArrayList<>();
        for (TC column : columns) {
            Objects.requireNonNull(column, "Column cannot be null");

            if (temp.contains(column))
                throw new IllegalArgumentException(
                        "Duplicate column: " + column.getName());
            temp.add(column);
        }
        this.columns = Collections.unmodifiableList(temp);

        if (primary_keys != null) {
            if (primary_keys.isEmpty())
                throw new IllegalArgumentException("Primary keys cannot be empty");
            ArrayList<TC> keys = new ArrayList<>(primary_keys);
            for (TC key : keys) {
                Objects.requireNonNull(key, "Primary key column cannot be null");

                if (!this.columns.contains(key)) {
                    throw new IllegalArgumentException(
                            "Primary key column must exist among provided columns: "
                                    + key.getName());
                }
            }
            this.primary_keys = Collections.unmodifiableList(keys);
        } else this.primary_keys = null;

        if (indexes != null) {
            List<TableIndex> filtered = new ArrayList<>();

            for (TableIndex index : indexes) {
                if (index.duplicatesPrimaryKey(this.primary_keys))
                    continue;

                filtered.add(index);
            }

            this.indexes = Collections.unmodifiableList(filtered);
        } else this.indexes = Collections.emptyList();
    }

    public BaseTableSchema(@NotNull String name, TypeRelationManager type_relation_manager, @NotNull List<TC> columns) {
        this(name, type_relation_manager, columns, null, null);
    }
    public BaseTableSchema(@NotNull String name, @NotNull List<TC> columns) {
        this(name, null, columns, null, null);
    }

    public String getName() {
        return name;
    }

    public @Unmodifiable @NotNull List<TC> getColumns() {
        return columns;
    }

    public @Nullable TC getColumn(String name) {
        for (TC column : columns)
            if (column.getName().equals(name))
                return column;
        return null;
    }

    public boolean hasPrimaryKey() {
        return primary_keys != null;
    }

    public @Unmodifiable @Nullable List<TC> getPrimaryKeys() {
        return primary_keys;
    }

    public boolean hasIndexes() {
        return indexes != null;
    }

    public @Nullable List<TableIndex> getIndexes() {
        return indexes;
    }

    public boolean hasTypeRelationManager() {
        return type_relation_manager != null;
    }
    public TypeRelationManager getTypeRelationManager() {
        return type_relation_manager;
    }

    public String createTableIfNotExistsSql() {
        return createTableIfNotExistsSql(false);
    }
    public String createTableIfNotExistsSql(boolean pretty) {
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS " + name + " (");
        if (pretty) sb.append("\n\t");
        sb.append(buildTableBody(pretty));
        if (hasPrimaryKey()) {
            sb.append(",");
            if (pretty) sb.append("\n\n\t");
            sb.append("PRIMARY KEY (");

            boolean comma = false;
            for (TC primary_key : primary_keys) {
                if (comma) sb.append(",");
                if (pretty) sb.append("\n\t\t");
                sb.append(primary_key.getName());
                comma = true;
            }
            if (pretty) sb.append("\n\t");
            sb.append(")");
            if (pretty) sb.append("\n");
        }
        return sb.append(')').toString();
    }

    public String buildTableBody() {
        return buildTableBody(false);
    }
    public String buildTableBody(boolean pretty) {
        StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for (TC column : columns) {
            if (comma) {
                sb.append(", ");
                if (pretty)
                    sb.append("\n\t");
            }
            sb.append(column.toColumnSql(primary_keys != null && primary_keys.contains(column)));
            comma = true;
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "TableSchema{" +
                "name='" + name + '\'' +
                ", columns=(" + columns +
                "), primaryKeys=" + primary_keys + "}";
    }


}
