package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.ResultSetMap;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface SimpleConvertingFunction<T> extends ConvertingFunction<T> {

    @Override
    default T apply(ResultSet resultSet, ResultSetMap map) throws SQLException {
        return apply(resultSet);
    }

    @Override
    T apply(ResultSet set) throws SQLException;

}
