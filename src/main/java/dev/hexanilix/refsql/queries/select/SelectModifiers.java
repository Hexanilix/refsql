package dev.hexanilix.refsql.queries.select;

import dev.hexanilix.refsql.TypeRelationManager;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectModifiers {

    private record Entry(String name, Object value) {}

    private List<Entry> whereEntries;
    private String orderBy;
    private long limit = -1;
    private long offset = -1;

    @Contract(" -> new")
    public static @NotNull SelectModifiers b() { return new SelectModifiers(); }

    public SelectModifiers where(String name, Object value) {
        if (whereEntries == null) whereEntries = new ArrayList<>();
        whereEntries.add(new Entry(name, value));
        return this;
    }

    public SelectModifiers orderBy(String column) {
        this.orderBy = column;
        return this;
    }
    public SelectModifiers orderBy(@NotNull TableColumn column) {
        this.orderBy = column.getName();
        return this;
    }
    public SelectModifiers limit(long limit) {
        this.limit = limit;
        return this;
    }

    public SelectModifiers offset(long offset) {
        this.offset = offset;
        return this;
    }

    public String build(TypeRelationManager mgr) {
        StringBuilder sb = new StringBuilder();
        boolean next = false;
        if (whereEntries != null && !whereEntries.isEmpty()) {
            sb.append("WHERE ");
            boolean and = false;
            for (Entry entry : whereEntries) {
                if (and) sb.append(" AND ");
                sb.append(entry.name).append('=');

                Object v = mgr != null ? mgr.convert(entry.value) : entry.value;
                if (v instanceof String) sb.append('\'').append((String) v).append('\'');
                else if (v instanceof TableColumn column) sb.append(column.getName());
                else sb.append(v);

                and = true;
            }
            next = true;
        }
        if (orderBy != null) {
            if (next) sb.append(' ');
            sb.append("ORDER BY ");
            sb.append(orderBy);
            next = true;
        }
        if (limit >= 0) {
            if (next) sb.append(' ');
            sb.append("LIMIT ").append(limit);
            next = true;
        }
        if (offset >= 0) {
            if (next) sb.append(' ');
            sb.append("OFFSET ").append(offset);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(
                SelectModifiers.b()
                        .where("id", 1)
                        .where("name", "test")
                        .orderBy("name")
                        .limit(10)
                        .offset(10)
                        .build(null)
        );
    }
}
