package dev.hexanilix.refsql.exceptions;

import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MissingColumnInResultSetException extends RuntimeException {

    public static final int NO_DEFAULT_VALUE = 0x1000000;

    @Contract("_, _, _ -> !null")
    private static @NotNull String buildMessage(@NotNull TableColumn column, @NotNull BaseTableSchema<?> schema, boolean tried_to_compensate) {
        return "Column " + column.getName() + " of " + schema.getName() + " was not found in the result set"
                + (tried_to_compensate ? " Attempted to compensate with default value, but one wasn't defined for this column" : "");
    }

    public MissingColumnInResultSetException(TableColumn column, BaseTableSchema<?> schema) {
        super(buildMessage(column, schema, false));
    }

    public MissingColumnInResultSetException(TableColumn column, BaseTableSchema<?> schema, boolean tried_to_compensate) {
        super(buildMessage(column, schema, tried_to_compensate));
    }

}
