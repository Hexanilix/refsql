package dev.hexanilix.refsql.queries;

import dev.hexanilix.refsql.table.TableColumn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class Upsert {

    public interface SqlQueryBuild { String build(); }

    public static final class Builder implements SqlQueryBuild {

        @Contract(value = "_ -> new", pure = true)
        public static @NotNull Builder n(String name) { return new Builder(name); }

        private final String name;
        private String on_conflict;
        private Collection<? extends TableColumn> columns;
        private Builder(String name) {
            this.name = name;
        }

        public Builder onConflict(String on_conflict) {
            this.on_conflict = on_conflict;
            return this;
        }

        public Builder columns(Collection<? extends TableColumn> columns) {
            this.columns = columns;
            return this;
        }

        public @NotNull String build() {
            StringBuilder upsert = new StringBuilder("INSERT INTO \"" + name + "\" (");

            boolean comma = false;
            for (TableColumn column : columns) {
                if (comma) upsert.append(", ");
                upsert.append('\"').append(column.getName()).append('\"');
                comma = true;
            }

            upsert.append(") VALUES (");

            comma = false;
            for (int i = 0; i < columns.size(); i++) {
                if (comma) upsert.append(", ");
                upsert.append("?");
                comma = true;
            }

            upsert.append(')');

            if (on_conflict != null)
                upsert.append(' ').append(on_conflict);

            return upsert.toString();
        }

    }

    public static final class ConfictUpdateSetBuilder implements SqlQueryBuild {
        private final List<? extends TableColumn> columns;
        private final List<? extends TableColumn> primary_keys;

        @Contract(value = "_, _ -> new", pure = true)
        public static @NotNull ConfictUpdateSetBuilder n(
                @NotNull List<? extends TableColumn> columns,
                @NotNull List<? extends TableColumn> primary_keys
        ) { return new ConfictUpdateSetBuilder(columns, primary_keys); }
        private ConfictUpdateSetBuilder(
                @NotNull List<? extends TableColumn> columns,
                @NotNull List<? extends TableColumn> primary_keys
        ) {
            this.columns = columns;
            this.primary_keys = primary_keys;
        }

        @Override
        public @NotNull String build() {
            StringBuilder upsert = new StringBuilder("ON CONFLICT (");

            boolean comma = false;
            for (TableColumn c : primary_keys) {
                if (comma) upsert.append(", ");
                upsert.append(c.getName());
                comma = true;
            }
            upsert.append(") DO UPDATE SET ");

            comma = false;
            for (TableColumn column : columns) {
                if (primary_keys.contains(column)) continue;

                if (comma) upsert.append(", ");
                upsert.append(column.getName()).append('=').append("EXCLUDED.").append(column.getName());
                comma = true;
            }

            return upsert.toString();
        }
    }

}
