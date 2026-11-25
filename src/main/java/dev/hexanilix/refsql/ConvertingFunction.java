package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.ResultSetColumnMetadata;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface ConvertingFunction<T> {
    T apply(ResultSet resultSet, Map<String, ResultSetColumnMetadata> meta) throws SQLException;
    default T apply(ResultSet resultSet) throws SQLException {
        return apply(resultSet, ResultSetColumnMetadata.buildMappings(resultSet));
    }

    default List<T> applyToList(@NotNull ResultSet resultSet, Map<String, ResultSetColumnMetadata> meta) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        while (resultSet.next())
            list.add(apply(resultSet, meta));
        return list;
    }
    default List<T> applyToList(@NotNull ResultSet resultSet) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        Map<String, ResultSetColumnMetadata> meta = ResultSetColumnMetadata.buildMappings(resultSet);
        while (resultSet.next())
            list.add(apply(resultSet, meta));
        return list;
    }

}
