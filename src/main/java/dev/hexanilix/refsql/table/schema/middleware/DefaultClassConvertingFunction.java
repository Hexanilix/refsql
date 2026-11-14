package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DefaultClassConvertingFunction<T> implements ConvertingFunction<T> {

    private record ColumnReadPair(FieldTableColumn column, ColumnReader reader) {};

    private final ColumnReadPair[] map;
    private final Constructor<T> constructor;
    public DefaultClassConvertingFunction(@NotNull ClassTableSchema<T> schema) {
        Class<T> clazz = schema.getEntityClass();
        try { constructor = clazz.getConstructor(); }
        catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Class " + clazz.getName()
                            + " does not have an accessible public no-argument constructor. "
                            + "Provide one or define a custom conversion function for this class.",
                    e
            );
        }
        map = new ColumnReadPair[schema.getColumns().size()];

        int i = 0;
        for (FieldTableColumn c : schema.getColumns()) {
            map[i++] = new ColumnReadPair(
                    c,
                    ColumnReader.createFor(
                            c.getName(),
                            c.getField().getType(),
                            schema.getTypeRelationManager()
                    )
            );
        }
    }

    @Override
    public T apply(ResultSet rs) throws SQLException {
        try {
            T t = constructor.newInstance();
            for (ColumnReadPair p : map)
                p.column.getField().set(t, p.reader.read(rs));
            return t;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
