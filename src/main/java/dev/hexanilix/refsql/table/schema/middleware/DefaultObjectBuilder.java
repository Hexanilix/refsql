package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.schema.middleware.base.ImplementedConvertingFunction;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnMapping;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnReader;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ResultSetColumnMetadata;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;

public class DefaultObjectBuilder<T> extends ImplementedConvertingFunction<ClassTableSchema<T>, T, T> {
    public DefaultObjectBuilder(@NotNull ClassTableSchema<T> schema) {
        super(schema);
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
        mappings = new ColumnMapping[schema.getColumns().size()];

        int i = 0;
        for (FieldTableColumn c : schema.getColumns()) {
            Field field = c.getField();
            if (!field.trySetAccessible())
                throw new IllegalArgumentException("Field " + field.getName() + " is not accessible to DefaultClassConvertingFunction. Ensure proper access or define a explicit converting function.");

            mappings[i++] = ColumnMapping.of(
                    c,
                    schema.getName(),
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

    public static Object getDefaultValueOnNotPresent(@NotNull TableColumn column, Class<?> clazz, BaseTableSchema<?> schema) throws MissingColumnInResultSetException, IllegalStateException {
        if (column instanceof FieldTableColumn ftc && !ftc.allowsNotPresent())
            throw new MissingColumnInResultSetException(column, schema);
        else if (!column.hasDefaultValue())
            throw new MissingColumnInResultSetException(column, schema, true);

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
    protected T createBase() throws Exception { return constructor.newInstance(); }

    @Override
    protected void applyToBase(T t, int index, TableColumn column, ResultSetColumnMetadata meta, Object value) throws IllegalAccessException {
        ((FieldTableColumn) column).getField().set(t, value);
    }

    @Override
    protected T buildObject(T t) { return t; }

}