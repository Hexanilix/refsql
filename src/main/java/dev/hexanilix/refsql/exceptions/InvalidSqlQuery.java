package dev.hexanilix.refsql.exceptions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class InvalidSqlQuery extends SqlExecutionError {

    @Contract("_, _, _ -> new")
    public static @NotNull String buildSqlErrorPointer(String query, int position, int padding) {
        char[] out = new char[(padding * 2 + 1) + padding];
        int j = 0;
        int i = 0;
        while (j < padding && position - i >= 0) {
            if (query.charAt(position - i) != '\n')
                out[(padding - 1) - (j++)] = query.charAt(position - i);
            i++;
        }
        i = 0;
        while (j < padding && position - i >= 0) {
            if (query.charAt(position - i) != '\n')
                out[(padding + 1) + (j++)] = query.charAt(position + i);
            i++;
        }
        return new String(out);
    }

    public static void main(String[] args) {
        System.out.println(buildSqlErrorPointer("0 1 2 3 4 5 6 7 8 9", 10, 4));
    }
    public InvalidSqlQuery(String query, int pos, Throwable cause) {
        super(query, "Failed to exec query: " + query, cause);
    }

}
