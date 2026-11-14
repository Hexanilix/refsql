package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.repo.ClassTableRepository;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.annotations.PrimaryKey;
import dev.hexanilix.refsql.table.schema.middleware.DefaultClassConvertingFunction;
import dev.hexanilix.refsql.table.schema.middleware.RecordClassConverter;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassTableSchema<T> extends BaseTableSchema<FieldTableColumn> {

    public static @NotNull List<Field> getAllFields(@NotNull Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Collections.addAll(fields, clazz.getFields());
        Collections.addAll(fields, clazz.getDeclaredFields());
        return fields;
    }

    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, (TypeRelationManager) null); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz, TypeRelationManager type_relation_manager)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, type_relation_manager); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull String name, @NotNull Class<T> clazz)
    { return of(name, clazz, (TypeRelationManager) null); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull String name, @NotNull Class<T> clazz, TypeRelationManager type_relation_manager)
    { return of(name, clazz, type_relation_manager, null); }

    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz, ConvertingFunction<T> func)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, func); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz, TypeRelationManager type_relation_manager, ConvertingFunction<T> func)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, type_relation_manager, func); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull String name, @NotNull Class<T> clazz, ConvertingFunction<T> func)
    { return of(name, clazz, null, func); }

    public static <T> @NotNull ClassTableSchema<T> of(
            @NotNull String name,
            @NotNull Class<T> clazz,
            TypeRelationManager type_relation_manager,
            ConvertingFunction<T> func
    ) {
        List<FieldTableColumn> columns = new ArrayList<>();
        List<FieldTableColumn> primary_keys = null;

        for (Field field : getAllFields(clazz)) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if (!clazz.isRecord() && (field.getModifiers() & Modifier.FINAL) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;

            FieldTableColumn c = FieldTableColumn.columnFrom(field, type_relation_manager);
            columns.add(c);
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (primary_keys == null) primary_keys = new ArrayList<>();
                primary_keys.add(c);
            }
        }

        return new ClassTableSchema<>(clazz, name, type_relation_manager, columns, primary_keys, func);
    }

    private final Class<T> clazz;
    private ConvertingFunction<T> converting_function;

    private ClassTableSchema(
            Class<T> clazz,
            @NotNull String name,
            @Nullable TypeRelationManager type_relation_manager,
            @NotNull List<FieldTableColumn> columns,
            @Nullable List<FieldTableColumn> primary_keys,
            @Nullable ConvertingFunction<T> func
    ) {
        super(name, type_relation_manager, columns, primary_keys);
        this.clazz = clazz;
        this.converting_function = func;
    }

    public @NotNull ConvertingFunction<T> getConvertingFunction() {
        return converting_function != null ? converting_function : (this.converting_function = buildConvertingFunction(this));
    }

    public static <T> @NotNull ConvertingFunction<T> buildConvertingFunction(@NotNull ClassTableSchema<T> schema) {
        Class<T> clazz = schema.clazz;
        if (clazz.isRecord())
            return (ConvertingFunction<T>) new RecordClassConverter<>((ClassTableSchema<? extends Record>) schema);
        return new DefaultClassConvertingFunction<>(schema);
    }

    public void setConvertingFunction(ConvertingFunction<T> func) {
        this.converting_function = func;
    }

    public ClassTableRepository<T> getRepository(Connection db) {
        return new ClassTableRepository<>(this, db);
    }

    public Class<T> getEntityClass() {
        return clazz;
    }
}
