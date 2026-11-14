package dev.hexanilix.refsql;

import dev.hexanilix.refsql.exceptions.SqlExecutionError;

import java.sql.SQLException;

public final class PostgresErrorHandler {

    private  PostgresErrorHandler() {}

    public static RuntimeException handle(String query, SQLException exception) {
        // Todo: Create a more robust exception system which parses the sql.
        //  Sql query errors in non-user provided sql are an internal error
        throw new SqlExecutionError(query, exception);
    }

}
