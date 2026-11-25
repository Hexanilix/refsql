package dev.hexanilix.refsql;

import dev.hexanilix.refsql.table.schema.middleware.ResultSetColumnMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

@FunctionalInterface
public interface SimpleConvertingFunction<T> extends ConvertingFunction<T> {

    @Override
    default T apply(ResultSet resultSet, Map<String, ResultSetColumnMetadata> meta) throws SQLException {
        return apply(resultSet);
    }

    @Override
    T apply(ResultSet set) throws SQLException;

}
