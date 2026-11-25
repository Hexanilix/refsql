package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.reflection.constructors.references.*;
import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ConstructorClassConverter<T> implements ConvertingFunction<T> {

    private static @NotNull Constructor<?> extractConstructor(@NotNull Serializable reference) {
        try {
            Method writeReplace = reference.getClass()
                    .getDeclaredMethod("writeReplace");

            writeReplace.setAccessible(true);

            SerializedLambda lambda =
                    (SerializedLambda) writeReplace.invoke(reference);

            if (lambda.getImplMethodKind() != MethodHandleInfo.REF_newInvokeSpecial)
                throw new IllegalArgumentException(
                        "Reference must be a constructor reference."
                );

            ClassLoader loader = Thread.currentThread().getContextClassLoader();

            Class<?> clazz = Class.forName(
                    lambda.getImplClass().replace('/', '.'),
                    false,
                    loader
            );

            MethodType methodType = MethodType.fromMethodDescriptorString(
                    lambda.getImplMethodSignature(),
                    loader
            );

            return clazz.getDeclaredConstructor(
                    methodType.parameterArray()
            );

        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(
                    "Could not extract constructor reference.",
                    e
            );
        }
    }


    private static final Pattern JAVA_COMPILER_ARG_REGEX = Pattern.compile("^arg\\d+$");

    private final BaseTableSchema<?> schema;
    private final ColumnMapping<?>[] mappings;
    private final Constructor<T> constructor;

    public ConstructorClassConverter(
            @NotNull BaseTableSchema<?> schema,
            @NotNull Class<T> clazz,
            @NotNull Constructor<T> constructor,
            @NotNull List<String> column_names
    ) {
        this.schema = schema;
        this.constructor = constructor;

        if (!constructor.getDeclaringClass().equals(clazz))
            throw new IllegalArgumentException(
                    "Constructor does not belong to " + clazz
            );

        Class<?>[] parameterTypes = constructor.getParameterTypes();

        if (column_names.size() != parameterTypes.length)
            throw new IllegalArgumentException(
                    "Expected " + parameterTypes.length + " parameter names, got " + column_names.size()
            );

        Map<String, TableColumn> columns = new HashMap<>();
        for (TableColumn column : schema.getColumns())
            columns.put(column.getName(), column);

        mappings = new ColumnMapping[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            String name = column_names.get(i);
            TableColumn column = columns.get(name);

            if (column == null) {
                String msg = "No column '" + name + "' found for parameter " + i + " (" + parameterTypes[i] + ") of constructor " + constructor + " in schema " + schema.getName();
                if (JAVA_COMPILER_ARG_REGEX.matcher(name).matches())
                    msg += ". This seems to be a parameter name rewritten by the java compiler, check if you added '-parameters' to your compiler args";

                throw new IllegalArgumentException(
                        msg
                );
            }

            mappings[i] = new ColumnMapping<>(
                    column,
                    ColumnReader.createFor(
                        column.getName(),
                        parameterTypes[i],
                        schema.getTypeRelationManager()
                    ),
                    parameterTypes[i]
            );
        }
    }

    public ConstructorClassConverter(
            @NotNull BaseTableSchema<?> schema,
            @NotNull Class<T> clazz,
            @NotNull Constructor<T> constructor
    ) {
        this(
                schema,
                clazz,
                constructor,
                Arrays.stream(constructor.getParameters())
                        .map(Parameter::getName)
                        .map(FieldTableColumn::toColumnName)
                        .toList()
        );
    }

    public ConstructorClassConverter(
            @NotNull BaseTableSchema<?> schema,
            @NotNull Constructor<T> constructor
    ) {
        this(
                schema,
                constructor.getDeclaringClass(),
                constructor,
                Arrays.stream(constructor.getParameters())
                        .map(Parameter::getName)
                        .map(FieldTableColumn::toColumnName)
                        .toList()
        );
    }

    public ConstructorClassConverter(
            @NotNull ClassTableSchema<T> schema,
            @NotNull Class<?>[] parameterTypes,
            @NotNull List<String> column_names
    ) {
        this(schema, schema.getEntityClass(), findConstructor(schema, parameterTypes), column_names);
    }

    private static <T> @NotNull Constructor<T> findConstructor(
            @NotNull ClassTableSchema<T> schema,
            Class<?>[] parameterTypes
    ) {
        try {
            return schema.getEntityClass().getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Class " + schema.getEntityClass().getName()
                            + " does not have a constructor with the specified parameter types.",
                    e
            );
        }
    }

    @Override
    public T apply(ResultSet rs, ResultSetMap map) throws SQLException {
        try {
            Object[] values = new Object[mappings.length];
            
            for (int i = 0; i < mappings.length; i++) {
                ColumnMapping<?> mapping = mappings[i];
                TableColumn column = mapping.column();
                String name = column.getName();

                ResultSetColumnMetadata meta = metas.get(name, schema.getName());
                if (meta != null) {
                    values[i] = mapping.reader().read(rs, meta);
                } else if (column.hasDefaultValue()) {
                    values[i] = DefaultClassConvertingFunction.getDefaultValueOnNotPresent(column, mapping.clazz(), schema);
                } else throw new MissingColumnInResultSetException(
                    "Column " + name + " of " + schema.getName() + " was not found in the result set. Attempted to compensate with default value, but one wasn't defined for this column"
                );
            }

            return constructor.newInstance(values);

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
    }


    // Constructor reference builders
    public ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor0<T> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor1<T, A1> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor2<T, A1, A2> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor3<T, A1, A2, A3> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor4<T, A1, A2, A3, A4> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor5<T, A1, A2, A3, A4, A5> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor6<T, A1, A2, A3, A4, A5, A6> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor7<T, A1, A2, A3, A4, A5, A6, A7> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor8<T, A1, A2, A3, A4, A5, A6, A7, A8> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor9<T, A1, A2, A3, A4, A5, A6, A7, A8, A9> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor10<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor11<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor12<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor13<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor14<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor15<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor16<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor17<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor18<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor19<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor20<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor21<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor22<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor23<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor24<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor25<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor26<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor27<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor28<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor29<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29, A30>
    ConstructorClassConverter(
            BaseTableSchema<?> schema,
            Constructor30<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29, A30> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }
}
