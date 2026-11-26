package dev.hexanilix.refsql.exceptions;

public class SqlResultError extends RuntimeException {
    public SqlResultError(String message) {
        super(message);
    }

    public SqlResultError(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlResultError(Throwable cause) {
        super(cause);
    }

    public SqlResultError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SqlResultError() {
    }

}
