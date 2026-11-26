package dev.hexanilix.refsql.table.schema.middleware.base;

import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnMapping;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ResultSetColumnMetadata;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;

public abstract class ConstructorImplementedConvertingFunction<Schema extends BaseTableSchema<?>, T> extends ImplementedConvertingFunction<Schema, Object[], T> {

    protected ConstructorImplementedConvertingFunction(Schema schema, Constructor<T> constructor, ColumnMapping<TableColumn>[] mappings) {
        super(schema, constructor, mappings);
    }
    protected ConstructorImplementedConvertingFunction(Schema schema, Constructor<T> constructor) {
        super(schema, constructor);
    }

    protected ConstructorImplementedConvertingFunction(Schema schema) {
        super(schema);
    }

    @Contract(value = " -> new", pure = true)
    protected Object @NotNull [] createBase() { return new Object[mappings.length]; }

    protected void applyToBase(Object @NotNull [] base, int index, TableColumn column, ResultSetColumnMetadata meta, Object value) {
        base[index] = value;
    }

    @Override
    protected T buildObject(Object[] objects) throws Exception {
        return constructor.newInstance(objects);
    }
}
