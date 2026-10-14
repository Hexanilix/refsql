package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.TableColumn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.Function;

public abstract class ClassSqlRelationDefinition<T> {

    public interface TOT<T> {
        T apply(ResultSet rs, String key) throws SQLException;
    }

    public static <T> ClassSqlRelationDefinition<T> textRelation(Function<T, ?> toSql, TOT<T> fromSql) {
        return new ClassSqlRelationDefinition<>(TableColumn.SqlType.TEXT) {
            @Override
            public T fromSql(ResultSet rs, String key) throws SQLException { return fromSql.apply(rs, key); }

            @Override
            public Object toSql(T t) { return toSql.apply(t); }
        };
    }

    private final TableColumn.SqlType sql_type;
    public ClassSqlRelationDefinition(TableColumn.SqlType sql_type) {
        this.sql_type = sql_type;
    }

    public abstract T fromSql(ResultSet rs, String key) throws SQLException;

    protected abstract Object toSql(T t);

    public Object convertToSql(T t) {
        Object value = toSql(t);
        if (TableColumn.SqlType.from(value.getClass()) != sql_type)
            throw new IllegalArgumentException("Invalid converted value in conversion definition for class "
                    + t.getClass() + ", got " + value.getClass() + " expected " + Arrays.toString(sql_type.getAliases()));
        return value;
    }

    public TableColumn.SqlType getSql_type() {
        return sql_type;
    }
}
