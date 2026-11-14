package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.reflection.constructors.references.*;
import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.TableColumn;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
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

    private final ColumnReader[] readers;
    private final Constructor<T> constructor;

    public ConstructorClassConverter(
            @NotNull BaseTableSchema<?> schema,
            @NotNull Class<?> clazz,
            @NotNull Constructor<T> constructor,
            @NotNull List<String> column_names
    ) {
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

        readers = new ColumnReader[parameterTypes.length];

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

            readers[i] = ColumnReader.createFor(
                    column.getName(),
                    parameterTypes[i],
                    schema.getTypeRelationManager()
            );
        }
    }

    public ConstructorClassConverter(
            @NotNull BaseTableSchema<?> schema,
            @NotNull Class<?> clazz,
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
            @NotNull ClassTableSchema<T> schema,
            @NotNull Constructor<T> constructor
    ) {
        this(
                schema,
                schema.getEntityClass(),
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
    public T apply(ResultSet rs) throws SQLException {
        try {
            Object[] values = new Object[readers.length];

            for (int i = 0; i < readers.length; i++)
                values[i] = readers[i].read(rs);

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
            ClassTableSchema<T> schema,
            Constructor0<T> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor1<T, A1> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor2<T, A1, A2> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor3<T, A1, A2, A3> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor4<T, A1, A2, A3, A4> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor5<T, A1, A2, A3, A4, A5> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor6<T, A1, A2, A3, A4, A5, A6> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor7<T, A1, A2, A3, A4, A5, A6, A7> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor8<T, A1, A2, A3, A4, A5, A6, A7, A8> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor9<T, A1, A2, A3, A4, A5, A6, A7, A8, A9> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor10<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor11<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor12<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor13<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor14<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor15<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor16<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor17<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor18<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor19<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor20<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor21<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor22<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor23<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor24<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor25<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor26<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor27<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor28<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor29<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }

    public <A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29, A30>
    ConstructorClassConverter(
            ClassTableSchema<T> schema,
            Constructor30<T, A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29, A30> reference
    ) { this(schema, (Constructor<T>) extractConstructor(reference)); }
}
