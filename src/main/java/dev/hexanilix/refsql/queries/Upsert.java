package dev.hexanilix.refsql.queries;

import dev.hexanilix.refsql.table.TableColumn;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import org.jetbrains.annotations.NotNull;

public class Upsert {

    public static <CT extends TableColumn> @NotNull String buildUpsertQuery(@NotNull BaseTableSchema<CT> schema) {
        StringBuilder upsert = new StringBuilder("INSERT INTO \"" + schema.getName() + "\" (");

        boolean comma = false;
        for (String names : schema.getColumnMap().keySet()) {
            if (comma) upsert.append(", ");
            upsert.append('\"').append(names).append('\"');
            comma = true;
        }

        upsert.append(") VALUES (");

        comma = false;
        for (int i = 0; i < schema.getColumnMap().size(); i++) {
            if (comma) upsert.append(", ");
            upsert.append("?");
            comma = true;
        }

        upsert.append(')');

        if (schema.hasPrimaryKey()) {
            upsert.append(" ON CONFLICT (").append(schema.getPrimaryKey().getName()).append(") DO UPDATE SET ");

            comma = false;
            for (TableColumn column : schema.getColumnMap().values()) {
                if (column == schema.getPrimaryKey()) continue;
                if (comma) upsert.append(", ");
                upsert.append(column.getName()).append('=').append("EXCLUDED.").append(column.getName());
                comma = true;
            }
        }

        return upsert.append(';').toString();
    }

}
