package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class RecordClassConverter<T extends Record> implements ConvertingFunction<T> {
    private final ColumnReader[] readers;
    private final Class<?>[] type_map;
    private final Constructor<T> constructor;
    public RecordClassConverter(@NotNull ClassTableSchema<T> schema) {
        Class<T> clazz = schema.getEntityClass();

        RecordComponent[] components = clazz.getRecordComponents();
        readers = new ColumnReader[components.length];
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

            readers[i] = ColumnReader.createFor(
                    ftc.getName(),
                    (type_map[i] = component.getType()),
                    schema.getTypeRelationManager()
            );
        }

        try {
            constructor = clazz.getDeclaredConstructor(type_map);
        }  catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T apply(ResultSet rs) throws SQLException {
        try {
            Object[] values = new Object[type_map.length];

            int i = 0;
            for (ColumnReader reader : readers)
                values[i++] = reader.read(rs);

            return constructor.newInstance(values);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
