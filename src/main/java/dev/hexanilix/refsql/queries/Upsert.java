package dev.hexanilix.refsql.queries;

import dev.hexanilix.refsql.table.TableColumn;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Upsert {

    public static <TC extends TableColumn> @NotNull String buildUpsertQuery(@NotNull BaseTableSchema<TC> schema) {
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

        List<TC> primaryKeys = schema.getPrimaryKeys();
        if (primaryKeys != null) {
            upsert.append(" ON CONFLICT (");

            comma = false;
            for (TC c : primaryKeys) {
                if (comma) upsert.append(", ");
                upsert.append(c.getName());
                comma = true;
            }
            upsert.append(") DO UPDATE SET ");

            comma = false;
            for (TC column : schema.getColumnMap().values()) {
                if (primaryKeys.contains(column)) continue;

                if (comma) upsert.append(", ");
                upsert.append(column.getName()).append('=').append("EXCLUDED.").append(column.getName());
                comma = true;
            }
        }

        return upsert.append(';').toString();
    }
}
