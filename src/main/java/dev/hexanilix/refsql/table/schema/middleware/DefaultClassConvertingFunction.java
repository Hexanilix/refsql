package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DefaultClassConvertingFunction<T> implements ConvertingFunction<T> {

    private final ColumnMapping<FieldTableColumn>[] readers;
    private final Constructor<T> constructor;
    public DefaultClassConvertingFunction(@NotNull ClassTableSchema<T> schema) {
        Class<T> clazz = schema.getEntityClass();
        try {
            constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Class " + clazz.getName()
                            + " does not have a no-argument constructor. "
                            + "Provide one or define a custom conversion function for this class.",
                    e
            );
        } catch (InaccessibleObjectException | SecurityException e) {
            throw new IllegalArgumentException(
                    "Cannot access the no-argument constructor of class "
                            + clazz.getName()
                            + " due to Java module access restrictions. "
                            + "Consider opening the package containing this class "
                            + "using --add-opens or module-info.java.",
                    e
            );
        }
        readers = new ColumnMapping[schema.getColumns().size()];

        int i = 0;
        for (FieldTableColumn c : schema.getColumns()) {
            Field field = c.getField();
            if (!field.trySetAccessible())
                throw new IllegalArgumentException("Field " + field.getName() + " is not accessible to DefaultClassConvertingFunction. Ensure proper access or define a explicit converting function.");

            readers[i++] = new ColumnMapping<>(
                    c,
                    ColumnReader.createFor(
                            c.getName(),
                            field.getType(),
                            schema.getTypeRelationManager()
                    ),
                    field.getType()
            );
        }
    }

    private static Class<?> wrapPrimitive(@NotNull Class<?> clazz) {
        if (!clazz.isPrimitive()) {
            return clazz;
        }

        if (clazz == long.class) return Long.class;
        if (clazz == int.class) return Integer.class;
        if (clazz == boolean.class) return Boolean.class;
        if (clazz == double.class) return Double.class;
        if (clazz == float.class) return Float.class;
        if (clazz == char.class) return Character.class;
        if (clazz == byte.class) return Byte.class;
        if (clazz == short.class) return Short.class;

        return clazz;
    }

    public static Object getDefaultValueOnNotPresent(@NotNull TableColumn column, Class<?> clazz) throws MissingColumnInResultSetException, IllegalStateException {
        if (column instanceof FieldTableColumn ftc && !ftc.allowsNotPresent()) {
            throw new MissingColumnInResultSetException(
                    "Column '" + column.getName() + "' was not found in the result set"
            );
        }

        Object value = column.getDefaultValue().getValue();

        Class<?> expectedType = wrapPrimitive(clazz);

        if (value != null && !expectedType.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException(
                    "Default value provider for column '" + column.getName()
                            + "' returned an incompatible type. Expected "
                            + clazz.getName()
                            + " but got " + value.getClass().getName()
            );
        }

        return value;
    }

    @Override
    public T apply(@NotNull ResultSet rs, Map<String, ResultSetColumnMetadata> metas) throws SQLException {
        try {
            T t = constructor.newInstance();

            for (ColumnMapping<FieldTableColumn> mapping : readers) {
                FieldTableColumn column = mapping.column();
                String name = column.getName();

                ResultSetColumnMetadata meta = metas.get(name);

                if (meta != null) {
                    column.getField().set(t, mapping.reader().read(rs, meta));
                } else if (column.hasDefaultValue()) {
                    column.getField().set(t, getDefaultValueOnNotPresent(column, mapping.clazz()));
                }
            }

            return t;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Constructor " + constructor + " threw an exception.",
                    e.getCause()
            );
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Could not instantiate using constructor " + constructor,
                    e
            );
        }
    }}