package dev.hexanilix.refsql.exceptions;

public class SqlExecutionError extends RuntimeException {

    private String query;
    public SqlExecutionError(String query, String message, Throwable cause) {
        super(message + ". Sql: " + query, cause);
    }
    public SqlExecutionError(String query, Throwable cause) {
        super("Failed to execute query: " + query, cause);
    }


    public String getQuery() {
        return query;
    }
}
