package dev.hexanilix.refsql.table.schema.column;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.annotations.Column;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FieldTableColumn extends TableColumn {

    public static @NotNull String toColumnName(String name) {
        return Cases.Camel.lower(name);
    }

    private static Object invokeSupplyingMethod(@NotNull Method method) {
        try {
            return method.invoke(null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class Builder {

        private final Field field;
        private SqlType type;
        private boolean not_null;
        private boolean unique =  false;
        private boolean allow_not_present = false;
        private @Nullable ColumnDefault<?> default_value;

        public Builder(@NotNull Field field, @Nullable TypeRelationManager manager, @Nullable Method supplying_method) {
            Class<?> type = field.getType();

            SqlType sqlType = null;
            if (manager != null) {
                ClassSqlRelationDefinition<?> def = manager.get(type);
                if (def != null)
                    sqlType = def.getSqlType();
            }
            if (sqlType == null && (sqlType = SqlType.from(type)) == null)
                throw new IllegalArgumentException(
                        "Class " + type.getCanonicalName() + " cannot conform to a Sql type. Create with a TypeRelationManager which has a sql type relation for this class"
                );

            this.field = field;
            this.type = sqlType;
            this.not_null = field.isAnnotationPresent(NotNull.class) || type.isPrimitive();

            String default_string_value;

            Column cd = field.getAnnotation(Column.class);
            if (cd != null) {
                this.not_null = this.not_null || !cd.nullable();
                this.unique = cd.unique();
                this.allow_not_present = cd.allowNotPresent();
                default_string_value = cd.def().isBlank() ? null : cd.def();
            } else {
                default_string_value = null;
            }

            if (default_string_value != null && supplying_method != null) {
                this.default_value = new ColumnDefault<>() {
                    @Override
                    public Object getValue() { return invokeSupplyingMethod(supplying_method); }
                    @Override
                    public String getAsSqlDefaultString() { return default_string_value; }
                };
            } else if (default_string_value != null) {
                this.default_value = new ColumnDefault<>() {
                    @Override
                    public Object getValue() { return default_string_value; }
                    @Override
                    public String getAsSqlDefaultString() { return default_string_value;}
                };
            } else if (supplying_method != null) {
                this.default_value = () -> invokeSupplyingMethod(supplying_method);
            }
        }

        public Builder sqlType(SqlType type) { this.type = type; return this; }

        public Builder notNull(boolean value) { this.not_null = value; return this; }
        public Builder unique(boolean value) { this.unique = value; return this; }
        public Builder allowNotPresent(boolean value) { this.allow_not_present = value; return this; }

        public Builder defaultValue(@Nullable ColumnDefault<?> default_value) { this.default_value = default_value; return this; }

        @Contract(" -> new")
        public @NotNull FieldTableColumn build() {
            return new FieldTableColumn(
                    field,
                    type,
                    not_null,
                    unique,
                    default_value,
                    allow_not_present
            );
        }
    }

    private final Field field;
    private final boolean allow_not_present;

    public FieldTableColumn(@NotNull Field field, @Nullable SqlType type, boolean not_null, boolean unique, @Nullable ColumnDefault<?> default_value, boolean allow_not_present) {
        super(toColumnName(field.getName()), type != null ? type : SqlType.from(field.getClass()), not_null, unique, default_value);
        this.field = field;
        this.allow_not_present = allow_not_present;
    }

    public Field getField() {
        return field;
    }

    public boolean allowsNotPresent() {
        return allow_not_present;
    }
}
