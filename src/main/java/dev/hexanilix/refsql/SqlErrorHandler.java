package dev.hexanilix.refsql;

import dev.hexanilix.refsql.exceptions.InvalidSqlQuery;
import dev.hexanilix.refsql.exceptions.SqlExecutionError;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.sql.SQLException;

public final class SqlErrorHandler {

    private SqlErrorHandler() {}

    public static SqlExecutionError handle(String query, SQLException exception) {
        // Todo: Create a more robust exception system which parses the sql.
        //  Sql query errors in non-user provided sql are an internal error

        String state = exception.getSQLState();

        if (exception instanceof PSQLException pse) {

            ServerErrorMessage error = pse.getServerErrorMessage();

            if (error != null) {

//                System.out.println("Message: " + error.getMessage());
//                System.out.println("SQL State: " + );
//
//                // Position in the SQL query (1-based)
//                System.out.println("SQL Position: " + error.getPosition());
//
//                System.out.println("Detail: " + error.getDetail());
//                System.out.println("Hint: " + error.getHint());
//
//                System.out.println("Where: " + error.getWhere());
//                System.out.println("Internal Query: " + error.getInternalQuery());
//                System.out.println("Internal Query Position: " + error.getInternalPosition());
//
//                System.out.println("Schema: " + error.getSchema());
//                System.out.println("Table: " + error.getTable());
//                System.out.println("Column: " + error.getColumn());
//                System.out.println("Constraint: " + error.getConstraint());

                if ("42601".equals(state))
                    return new InvalidSqlQuery(query, error.getPosition(), pse);
            }
        }

        throw new SqlExecutionError(query, exception);
    }

}
