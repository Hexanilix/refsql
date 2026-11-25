package dev.hexanilix.refsql.table.schema.column;

import dev.hexanilix.refsql.SqlType;

@FunctionalInterface
public interface ColumnDefault<T> {

    T getValue();

    /**
     *
     * @return A valid sql column default value
     */
    default String getAsSqlDefaultString() {
        return SqlType.wrapRawValueForQuery(String.valueOf(getValue()));
    }

}
