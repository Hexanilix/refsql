package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.ResultSetMap;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface ConvertingFunction<T> {
    T apply(ResultSet resultSet, ResultSetMap map) throws SQLException;
    default T apply(ResultSet resultSet) throws SQLException {
        return apply(resultSet, new ResultSetMap(resultSet));
    }

    default List<T> applyToList(@NotNull ResultSet resultSet, ResultSetMap map) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        while (resultSet.next())
            list.add(apply(resultSet, map));
        return list;
    }
    default List<T> applyToList(@NotNull ResultSet resultSet) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        ResultSetMap map = new ResultSetMap(resultSet);
        while (resultSet.next())
            list.add(apply(resultSet, map));
        return list;
    }

}
