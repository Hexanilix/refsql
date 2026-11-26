package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.middleware.base.ConstructorImplementedConvertingFunction;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnMapping;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnReader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.RecordComponent;
import java.util.Collection;

public class RecordObjectBuilder<T extends Record> extends ConstructorImplementedConvertingFunction<ClassTableSchema<T>, T> {

    private final Class<?>[] type_map;
    public RecordObjectBuilder(@NotNull ClassTableSchema<T> schema) {
        super(schema);
        Class<T> clazz = schema.getEntityClass();

        RecordComponent[] components = clazz.getRecordComponents();
        mappings = new ColumnMapping[components.length];
        type_map = new Class<?>[components.length];

        Collection<FieldTableColumn> columns = schema.getColumns();
        for (FieldTableColumn ftc : columns) {

            int i = 0;
            RecordComponent component = null;
            for (RecordComponent rc : components) {
                if (rc.getName().equals(ftc.getField().getName())) {
                    component = rc;
                    break;
                }
                i++;
            }

            if (component == null)
                throw new IllegalStateException("No record component for column " + ftc);

            mappings[i] = ColumnMapping.of(
                    ftc,
                    schema.getName(),
                    ColumnReader.createFor(
                            ftc.getName(),
                            (type_map[i] = component.getType()),
                            schema.getTypeRelationManager()
                    ),
                    component.getType()
            );
        }

        try {
            constructor = clazz.getDeclaredConstructor(type_map);
            constructor.trySetAccessible();
        }  catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Object @NotNull [] createBase() { return new Object[type_map.length]; }

}
