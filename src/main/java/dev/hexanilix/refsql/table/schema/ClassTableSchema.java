package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.repo.ClassTableRepository;
import dev.hexanilix.refsql.table.annotations.PrimaryKey;
import dev.hexanilix.refsql.table.schema.middleware.RecordClassConverter;
import dev.hexanilix.refsql.table.schema.middleware.DefaultClassConvertingFunction;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.*;
import java.sql.Connection;

public class ClassTableSchema<T> extends BaseTableSchema<FieldTableColumn> {

    private final Class<T> clazz;
    private ConvertingFunction<T> convertToTFromRs;
    public ClassTableSchema(@NotNull Class<T> clazz) {
        this(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz);
    }
    public ClassTableSchema(@NotNull Class<T> clazz, TypeRelationManager type_relation_manager) {
        this(Cases.Camel.lower(clazz.getSimpleName()) + "s", clazz, type_relation_manager);
    }
    public ClassTableSchema(String name, @NotNull Class<T> clazz) { this(name, clazz, null); }
    public ClassTableSchema(String name, @NotNull Class<T> clazz, TypeRelationManager type_relation_manager) {
        super(name, type_relation_manager);
        this.clazz = clazz;
        Field[] fields  = clazz.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
            if (!clazz.isRecord() && (field.getModifiers() & Modifier.FINAL) != 0) continue;
            if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;

            FieldTableColumn c = FieldTableColumn.columnFrom(field, type_relation_manager);
            this.columns.put(c.getName(), c);
            if (field.isAnnotationPresent(PrimaryKey.class)) {
                if (this.primary_key == null)
                    this.primary_key = c;
                else
                    throw new IllegalStateException("Primary key already exists in table " + this.name + ": " + this.primary_key);
            }
        }
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
