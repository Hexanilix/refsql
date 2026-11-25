package dev.hexanilix.refsql.exceptions;

public class MissingColumnInResultSetException extends RuntimeException {
    public MissingColumnInResultSetException(String message) {
        super(message);
    }
}
