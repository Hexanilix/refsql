package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.Pair;
import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class DefaultClassConvertingFunction<T> implements ConvertingFunction<T> {

    private final Pair<FieldTableColumn, ColumnReader>[] readers;
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
        readers = new Pair[schema.getColumns().size()];

        int i = 0;
        for (FieldTableColumn c : schema.getColumns()) {
            readers[i++] = Pair.of(
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
    public T apply(@NotNull ResultSet rs) throws SQLException {
        try {
            T t = constructor.newInstance();
            Set<String> columnNames = ColumnReader.getColumns(rs);

            for (Pair<FieldTableColumn, ColumnReader> p : readers) {
                String columnName = p.key().getName();

                if (columnNames.contains(columnName)) {
                    p.key().getField().set(t, p.value().read(rs));
                } else if (!p.key().allowsNotPresent()) {
                    throw new MissingColumnInResultSetException(
                            "Column " + columnName + " was not found in the result set"
                    );
                }
            }

            return t;
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}