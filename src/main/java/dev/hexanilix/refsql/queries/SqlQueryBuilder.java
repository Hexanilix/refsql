package dev.hexanilix.refsql.queries;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class SqlQueryBuilder {

    private static class Joiner {
        private final StringBuilder string = new StringBuilder();
        private String joiner;
        private int count = 0;
        public Joiner(String joiner) {
            this.joiner = joiner;
        }
        public Joiner add(String value) {
            if (count > 0) string.append(joiner);
            string.append(value);
            count++;
            return this;
        }

        public boolean isEmpty() {
            return count == 0;
        }

        @Override
        public String toString() {
            return string.toString();
        }
    }

    private Joiner where = new Joiner(" AND ");
    private Joiner order_by = new Joiner(", ");
    private int offset = -1;
    private int limit = -1;
    private SqlQueryBuilder() {}
    @Contract(value = " -> new", pure = true)
    public static @NotNull SqlQueryBuilder n() { return new SqlQueryBuilder(); }

    public SqlQueryBuilder where(String expression) { this.where.add(expression); return this; }

    public SqlQueryBuilder whereP(String column) { return where(column, "?"); }
    public SqlQueryBuilder where(String column, String value) {
        this.where.add(column + " = " + value);
        return this;
    }

    public SqlQueryBuilder orderBy(String @NotNull ... columns) {
        for (String column : columns)
            this.order_by.add(column);
        return this;
    }

    public SqlQueryBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }


    public SqlQueryBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public String buildBody(boolean include_positioning) {
        String body = "";
        if (!where.isEmpty()) body += " WHERE " + where;

        if (!order_by.isEmpty()) body += " ORDER BY " + order_by;

        if (include_positioning) {
            if (offset > -1) body += " OFFSET " + offset;
            if (limit > -1) body += " LIMIT " + limit;
        }

        return body;
    }

    public String select(String table) { return select("*", table); }
    public String select(String rows, String table) {
        return "SELECT " + rows + " FROM " + table + buildBody(true);
    }

    public String count(String table) {
        return count("*", table);
    }
    public String count(String column, String table) {
        return "SELECT COUNT(" + column + ") FROM " + table + (where.isEmpty() ? "" : " WHERE " + where);
    }

    public static void main(String[] args) {
        System.out.println(
                SqlQueryBuilder.n()
                    .whereP("character_id")
                    .whereP("pose_id")
                    .orderBy("created_at DESC")
                    .orderBy("id DESC")
                    .offset(20)
                    .limit(10)

                    .select("*", ":name")
        );
    }

}
