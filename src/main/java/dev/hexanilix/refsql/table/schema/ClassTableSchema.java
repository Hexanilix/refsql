package dev.hexanilix.refsql.table.schema;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.repo.ClassTableRepository;
import dev.hexanilix.refsql.table.TableIndex;
import dev.hexanilix.refsql.table.annotations.ColumnDefaultSupplier;
import dev.hexanilix.refsql.table.annotations.Index;
import dev.hexanilix.refsql.table.annotations.PrimaryKey;
import dev.hexanilix.refsql.table.annotations.SchemaIgnore;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.schema.middleware.DefaultClassConvertingFunction;
import dev.hexanilix.refsql.table.schema.middleware.RecordClassConverter;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.util.*;
import java.util.function.Function;

public class ClassTableSchema<T> extends BaseTableSchema<FieldTableColumn> {

    public static @NotNull List<Field> getAllFields(@NotNull Class<?> clazz) {
        Set<Field> fields = new LinkedHashSet<>();

        Collections.addAll(fields, clazz.getFields());
        Collections.addAll(fields, clazz.getDeclaredFields());

        return new ArrayList<>(fields);
    }

    public static @NotNull String toTableName(@NotNull Class<?> clazz) {
        String base = Cases.Camel.lower(clazz.getSimpleName());
        if (base.charAt(base.length() - 1) != 's') base += "s";
        return base;
    }

    public static final class Builder<T> {

        private String name;
        private TypeRelationManager type_relation_manager = null;
        private ConvertingFunction<T> func = null;
        private Function<? super BaseTableSchema<?>, ? extends ConvertingFunction<T>> func_to_resolve = null;
        private List<TableIndex> indexes = null;
        private List<String> indexes_to_resolve = null;
        private final Class<T> clazz;

        @Contract(value = "_ -> new", pure = true)
        public static <T> @NotNull Builder<T> of(Class<T> clazz) {
            return new Builder<>(clazz);
        }
        @Contract(value = "_, _ -> new", pure = true)
        public static <T> @NotNull Builder<T> of(String name, Class<T> clazz) {
            return new Builder<>(name, clazz);
        }
        public Builder(Class<T> clazz) {
            this(null, clazz);
        }

        public Builder(String name, Class<T> clazz) {
            this.name = name;
            this.clazz = clazz;
        }

        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        public Builder<T> conv(ConvertingFunction<T> func) {
            this.func = func;
            return this;
        }
        public Builder<T> convFunc(Function<? super BaseTableSchema<?>, ? extends ConvertingFunction<T>> func) {
            this.func_to_resolve = func;
            return this;
        }

        public Builder<T> manager(TypeRelationManager type_relation_manager) {
            this.type_relation_manager = type_relation_manager;
            return this;
        }

        public Builder<T> indexes(List<TableIndex> indexes) {
            this.indexes = indexes;
            return this;
        }
        public Builder<T> index(TableIndex index) {
            if (this.indexes == null)
                this.indexes = new ArrayList<>();
            this.indexes.add(index);
            return this;
        }

        public Builder<T> index(String column_name) {
            if (indexes_to_resolve == null)
                indexes_to_resolve = new ArrayList<>();
            this.indexes_to_resolve.add(column_name);
            return this;
        }

        private record PKMapping(
                Field field,
                Integer pos,
                FieldTableColumn column
        ) {}
        @Contract(" -> new")
        public @NotNull ClassTableSchema<T> build() {
            List<FieldTableColumn> columns = new ArrayList<>();
            Map<String, PKMapping> primary_keys = null;

            Map<String, Method> default_suppliers = new HashMap<>();

            for (Method m : clazz.getDeclaredMethods()) {
                ColumnDefaultSupplier sup = m.getAnnotation(ColumnDefaultSupplier.class);
                if (sup != null) {
                    if ((m.getModifiers() & Modifier.STATIC) == 0)
                        throw new IllegalArgumentException("@ColumnDefaultSupplier function " + m
                                + " must be static");

                    Method other;
                    if ((other = default_suppliers.put(sup.field(), m)) != null) {
                        throw new IllegalArgumentException("Default field value supplier method defined for field '" + sup.field()
                                + "' more than once. Problem methods: " + other + ", " + m);
                    }
                }
            }

            for (Field field : getAllFields(clazz)) {
                if ((field.getModifiers() & Modifier.STATIC) != 0) continue;
                if (!clazz.isRecord() && (field.getModifiers() & Modifier.FINAL) != 0) continue;
                if ((field.getModifiers() & Modifier.TRANSIENT) != 0) continue;

                SchemaIgnore ignore = field.getAnnotation(SchemaIgnore.class);
                if (ignore != null) { default_suppliers.remove(field.getName()); continue; }

                Method default_supplier = default_suppliers.remove(field.getName());
                if (default_supplier != null) {
                    if (!default_supplier.trySetAccessible())
                        throw new IllegalArgumentException(
                                "Default field value method " + default_supplier + " is not accessible"
                        );
                    if (!field.getType().isAssignableFrom(default_supplier.getReturnType())) {
                        throw new IllegalArgumentException(
                                "Default field value method " + default_supplier + " return type is incompatible with field "
                                        + field + " type. Expected " + field.getType() + " got " + default_supplier.getReturnType()
                        );
                    }
                }

                FieldTableColumn column = new FieldTableColumn.Builder(
                        field,
                        type_relation_manager,
                        default_supplier
                ).build();

                columns.add(column);
                PrimaryKey primary_key_annon = field.getAnnotation(PrimaryKey.class);
                if (primary_key_annon != null) {
                    if (primary_keys == null)
                        primary_keys = new HashMap<>();
                    var prev = primary_keys.put(column.getName(), new PKMapping(field, primary_key_annon.position(), column));
                    if (prev != null) {
                        throw new IllegalStateException("Duplicate primary key name. First derived from " + prev.field + ", then " + field);
                    }
                }

                Index index_annon = field.getAnnotation(Index.class);
                if (index_annon != null) {
                    if (indexes == null)
                        indexes = new ArrayList<>();
                    indexes.add(new TableIndex(new TableColumn[]{ column }, index_annon.modif()));
                }
            }

            if (!default_suppliers.isEmpty()) {
                throw new IllegalStateException("Default column value supplier " + default_suppliers + " fields not found in class " + clazz);
            }

            if (name == null) name = toTableName(clazz);

            List<TableIndex> resolved_indexes = indexes != null ? new ArrayList<>(indexes) : new ArrayList<>();

            if (indexes_to_resolve != null) for (String index : indexes_to_resolve) {
                boolean resolved = false;
                for (TableColumn column : columns) {
                    if (column.getName().equalsIgnoreCase(index)) {
                        resolved_indexes.add(
                                new TableIndex(column)
                        );
                        resolved = true;
                        break;
                    }
                }
                if (!resolved) {
                    throw new IllegalArgumentException("Invalid index column name: " + index);
                }
            }

            List<FieldTableColumn> ordered_primary_keys = null;

            if (primary_keys != null) {
                ordered_primary_keys = new ArrayList<>(
                        Collections.nCopies(primary_keys.size(), null)
                );

                List<FieldTableColumn> to_add = new ArrayList<>();

                for (PKMapping mapping : primary_keys.values()) {
                    int position = mapping.pos;
                    FieldTableColumn pk = mapping.column;

                    if (position == -1) {
                        to_add.add(pk);
                        continue;
                    }

                    if (position < 0 || position >= ordered_primary_keys.size())
                        throw new IllegalArgumentException(
                                "Invalid index for primary key: " + pk.getName()
                        );

                    FieldTableColumn existing = ordered_primary_keys.get(position);

                    if (existing != null)
                        throw new IllegalArgumentException(
                                "Primary key " + pk.getName()
                                        + " index of " + position
                                        + " is already occupied by primary key "
                                        + existing.getName()
                        );

                    ordered_primary_keys.set(position, pk);
                }

                int nextPosition = 0;

                for (FieldTableColumn pk : to_add) {
                    while (nextPosition < ordered_primary_keys.size() && ordered_primary_keys.get(nextPosition) != null) {
                        nextPosition++;
                    }

                    if (nextPosition >= ordered_primary_keys.size())
                        throw new IllegalArgumentException(
                                "Unable to assign position to primary key: " + pk.getName()
                        );

                    ordered_primary_keys.set(nextPosition++, pk);
                }
            }

            if (func_to_resolve != null) {
                return new ClassTableSchema<>(
                        clazz,
                        name,
                        type_relation_manager,
                        columns,
                        ordered_primary_keys,
                        func_to_resolve,
                        resolved_indexes
                );
            } else {
                return new ClassTableSchema<>(
                        clazz,
                        name,
                        type_relation_manager,
                        columns,
                        ordered_primary_keys,
                        func,
                        resolved_indexes
                );
            }
        }
    }

    private final Class<T> clazz;
    private ConvertingFunction<T> converting_function;

    private ClassTableSchema(
            Class<T> clazz,
            @NotNull String name,
            @Nullable TypeRelationManager type_relation_manager,
            @NotNull List<FieldTableColumn> columns,
            @Nullable List<FieldTableColumn> primary_keys,
            @Nullable ConvertingFunction<T> func,
            @Nullable List<TableIndex> indexes
    ) {
        super(name, type_relation_manager, columns, primary_keys, indexes);
        this.clazz = clazz;
        this.converting_function = func;
    }
    private ClassTableSchema(
            Class<T> clazz,
            @NotNull String name,
            @Nullable TypeRelationManager type_relation_manager,
            @NotNull List<FieldTableColumn> columns,
            @Nullable List<FieldTableColumn> primary_keys,
            @Nullable Function<? super BaseTableSchema<?>, ? extends ConvertingFunction<T>> func,
            @Nullable List<TableIndex> indexes
    ) {
        super(name, type_relation_manager, columns, primary_keys, indexes);
        this.clazz = clazz;
        this.converting_function = func != null ? func.apply(this) : null;
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
