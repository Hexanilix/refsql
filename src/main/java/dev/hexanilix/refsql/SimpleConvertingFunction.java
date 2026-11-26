package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.ResultSetMap;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@FunctionalInterface
public interface SimpleConvertingFunction<T> extends ConvertingFunction<T> {

    @Override
    default T apply(ResultSet resultSet, ResultSetMap map) throws SQLException {
        return apply(resultSet);
    }

    @Override
    default List<T> applyToList(@NotNull ResultSet resultSet) throws SQLException {
        ArrayList<T> list = new ArrayList<>();
        while (resultSet.next())
            list.add(apply(resultSet));
        return list;
    }

    @Override
    T apply(ResultSet set) throws SQLException;

}
