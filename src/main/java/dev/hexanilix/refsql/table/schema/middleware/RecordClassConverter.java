package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class RecordClassConverter<T extends Record> implements ConvertingFunction<T> {

    private final ClassTableSchema<T> schema;
    private final ColumnMapping<FieldTableColumn>[] mappings;
    private final Class<?>[] type_map;
    private final Constructor<T> constructor;
    public RecordClassConverter(@NotNull ClassTableSchema<T> schema) {
        this.schema = schema;
        Class<T> clazz = schema.getEntityClass();

        RecordComponent[] components = clazz.getRecordComponents();
        mappings = new ColumnMapping[components.length];
        type_map = new Class<?>[components.length];

        Collection<FieldTableColumn> columns = schema.getColumns();
        for (FieldTableColumn ftc : columns) {

            int i = 0;
            RecordComponent component = null;
            for (RecordComponent rc : components) {
                if (rc.getName().equals(ftc.getField().getName())) {
                    component = rc;
                    break;
                }
                i++;
            }

            if (component == null)
                throw new IllegalStateException("No record component for column " + ftc);

            mappings[i] = new ColumnMapping<>(
                    ftc,
                    ColumnReader.createFor(
                        ftc.getName(),
                        (type_map[i] = component.getType()),
                        schema.getTypeRelationManager()
                    ),
                    component.getType()
            );
        }

        try {
            constructor = clazz.getDeclaredConstructor(type_map);
        }  catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T apply(ResultSet rs, ResultSetMap map) throws SQLException {
        try {
            Object[] values = new Object[type_map.length];

            for (int i = 0; i < mappings.length; i++) {
                ColumnMapping<FieldTableColumn> mapping = mappings[i];
                FieldTableColumn column = mapping.column();
                String name = mapping.column().getName();

                ResultSetColumnMetadata meta = metas.get(name, schema.getName());
                if (meta != null) {
                    values[i] = mapping.reader().read(rs, meta);
                } else if (column.hasDefaultValue()) {
                    values[i] = DefaultClassConvertingFunction.getDefaultValueOnNotPresent(column, mapping.clazz(), schema);
                } else throw new MissingColumnInResultSetException(
                        "Column " + name + " was not found in the result set. Attempted to compensate with default value, but one wasn't defined for this column"
                );
            }

            return constructor.newInstance(values);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
