package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.Pair;
import dev.hexanilix.buffers.crono.ChronoTest;
import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.repo.ClassTableRepository;
import dev.hexanilix.refsql.table.annotations.PrimaryKey;
import dev.hexanilix.refsql.table.schema.middleware.RecordClassConverter;
import dev.hexanilix.refsql.table.schema.middleware.DefaultClassConvertingFunction;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class ClassTableSchema<T> extends BaseTableSchema<FieldTableColumn> {

    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, null); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull Class<T> clazz, TypeRelationManager type_relation_manager)
    { return of(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, type_relation_manager); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull String name, @NotNull Class<T> clazz)
    { return of(name, clazz, null); }
    public static <T> @NotNull ClassTableSchema<T> of(@NotNull String name, @NotNull Class<T> clazz, TypeRelationManager type_relation_manager) {
        List<FieldTableColumn> columns = new ArrayList<>();
        List<FieldTableColumn> primary_keys = null;

        Field[] fields  = clazz.getDeclaredFields();
        for (Field field : fields) {
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

        return new ClassTableSchema<>(clazz, name, type_relation_manager, columns, primary_keys);
    }

    private final Class<T> clazz;
    private ConvertingFunction<T> convertToTFromRs;

    private ClassTableSchema(
            Class<T> clazz,
            @NotNull String name,
            @Nullable TypeRelationManager type_relation_manager,
            @NotNull List<FieldTableColumn> columns,
            @Nullable List<FieldTableColumn> primary_keys) {
        super(name, type_relation_manager, columns, primary_keys);
        this.clazz = clazz;
    }

    public ConvertingFunction<T> getConvertingFunction() {
        return convertToTFromRs == null ? (convertToTFromRs = buildConvertingFunction(this)) :  convertToTFromRs;
    }

    public void initializeConvertingFunction() {
        if (convertToTFromRs == null)
            convertToTFromRs = buildConvertingFunction(this);
    }

    public static <T> @NotNull ConvertingFunction<T> buildConvertingFunction(@NotNull ClassTableSchema<T> schema) {
        Class<T> clazz = schema.clazz;
        if (clazz.isRecord())
            return (ConvertingFunction<T>) new RecordClassConverter<>((ClassTableSchema<? extends Record>) schema);
        return new DefaultClassConvertingFunction<>(schema);
    }

    public void setConvertingFunction(ConvertingFunction<T> func) {
        this.convertToTFromRs = func;
    }

    public ClassTableRepository<T> getRepository(Connection db) {
        return new ClassTableRepository<>(this, db);
    }

    public Class<T> getEntityClass() {
        return clazz;
    }
}
