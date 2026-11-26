package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.mapping.QueryResult;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ResultSetColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface ConvertingFunction<T> {
    T apply(QueryResult result) throws SQLException;
    default T apply(QueryResult result, @Nullable ResultSetColumnMetadata[] precompute) throws SQLException {
        return apply(result);
    }
    default List<T> applyToList(@NotNull QueryResult result) throws SQLException {
        ArrayList<T> list = new ArrayList<>();
        while (result.next())
            list.add(apply(result));
        return list;
    }

    default @Nullable ResultSetColumnMetadata[] getComputedMappings(@NotNull QueryResult qr) throws SQLException { return null; }

    // ResultSet delegation
    default T apply(ResultSet rs) throws SQLException { return apply(new QueryResult(rs)); }

    default List<T> applyToList(@NotNull ResultSet rs) throws SQLException { return applyToList(new QueryResult(rs)); }

}
