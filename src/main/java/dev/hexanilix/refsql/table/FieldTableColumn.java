package dev.hexanilix.refsql.table;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.annotations.Column;
import dev.hexanilix.text.style.Cases;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class FieldTableColumn extends TableColumn {

    @Contract("_ -> new")
    public static @NotNull FieldTableColumn columnFrom(@NotNull Field field) { return columnFrom(field, null); }
    @Contract("_, _ -> new")
    public static @NotNull FieldTableColumn columnFrom(@NotNull Field field, TypeRelationManager manager) {
        Class<?> type = field.getType();

        SqlType sqlType = null;
        if (manager != null) {
            ClassSqlRelationDefinition<?> def = manager.get(type);
            if (def != null)
                sqlType = def.getSql_type();
        }
        if (sqlType == null) sqlType = SqlType.from(type);

        FieldTableColumn c;
        try {
             c = new FieldTableColumn(field, sqlType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Class " + type.getCanonicalName() + " cannot conform to a Sql type. Creating with a TypeRelationManager which has a sql type relation for this class"
            );
        }

        Column cd = field.getAnnotation(Column.class);
        if (cd != null) {
            c.setDefaultValue(cd.def());
            c.getModifiers().notNull(!cd.nullable());
        }

        if (field.isAnnotationPresent(NotNull.class) || type.isPrimitive())
            c.getModifiers().notNull(true);

        return c;
    }


    private final Field field;
    public FieldTableColumn(@NotNull Field field, Modifiers modifiers) throws IllegalArgumentException {
        super(Cases.Camel.lower(field.getName()), field.getType(), modifiers);
        this.field = field;
    }

    public FieldTableColumn(@NotNull Field field, @NotNull SqlType type, Modifiers modifiers) {
        super(Cases.Camel.lower(field.getName()), type, modifiers);
        this.field = field;
    }

    public FieldTableColumn(Field field, SqlType type) { this(field, type, null); }
    public FieldTableColumn(Field field) { this(field, (Modifiers) null); }

    public Field getField() {
        return field;
    }
}
