package dev.hexanilix.refsql.table.schema.middleware.mapping;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public final class ResultSetColumnMetadata {

    // Fetched flags (bits 0..17)
    private static final int FETCHED_NAME                = 1;
    private static final int FETCHED_LABEL               = 1 << 1;
    private static final int FETCHED_TYPE                = 1 << 2;
    private static final int FETCHED_TYPE_NAME           = 1 << 3;
    private static final int FETCHED_PRECISION           = 1 << 4;
    private static final int FETCHED_SCALE               = 1 << 5;
    private static final int FETCHED_TABLE               = 1 << 6;
    private static final int FETCHED_SCHEMA              = 1 << 7;
    private static final int FETCHED_CATALOG             = 1 << 8;
    private static final int FETCHED_NULLABLE            = 1 << 9;
    private static final int FETCHED_SIGNED              = 1 << 10;
    private static final int FETCHED_AUTO_INCREMENT      = 1 << 11;
    private static final int FETCHED_CASE_SENSITIVE      = 1 << 12;
    private static final int FETCHED_SEARCHABLE          = 1 << 13;
    private static final int FETCHED_READ_ONLY           = 1 << 14;
    private static final int FETCHED_WRITABLE            = 1 << 15;
    private static final int FETCHED_DEFINITELY_WRITABLE = 1 << 16;
    private static final int FETCHED_DISPLAY_SIZE        = 1 << 17;

    // Cached boolean values (bits 18..24)
    private static final int VALUE_SIGNED                = 1 << 18;
    private static final int VALUE_AUTO_INCREMENT        = 1 << 19;
    private static final int VALUE_CASE_SENSITIVE        = 1 << 20;
    private static final int VALUE_SEARCHABLE            = 1 << 21;
    private static final int VALUE_READ_ONLY             = 1 << 22;
    private static final int VALUE_WRITABLE              = 1 << 23;
    private static final int VALUE_DEFINITELY_WRITABLE   = 1 << 24;

    private final @NotNull ResultSetMetaData metadata;
    private final int index;

    private int state;

    private String name;
    private String label;
    private String typeName;
    private String table;
    private String schema;
    private String catalog;

    private int type;
    private int precision;
    private int scale;
    private int nullable;
    private int displaySize;

    public ResultSetColumnMetadata(@NotNull ResultSetMetaData metadata, int index) {
        this.metadata = metadata;
        this.index = index;
    }

    public ResultSetColumnMetadata(@NotNull ResultSetMetaData metadata, int index, @NotNull String label) {
        this.metadata = metadata;
        this.index = index;
        this.label = label;
        this.state = FETCHED_LABEL;
    }

    public static @NotNull ResultSetColumnMetadata of(@NotNull ResultSetMetaData metadata, int index) {
        return new ResultSetColumnMetadata(metadata, index);
    }

    public int index() {
        return index;
    }

    public @NotNull String name() throws SQLException {
        if ((state & FETCHED_NAME) == 0) {
            name = metadata.getColumnName(index);
            state |= FETCHED_NAME;
        }
        return name;
    }

    public @NotNull String label() throws SQLException {
        if ((state & FETCHED_LABEL) == 0) {
            label = metadata.getColumnLabel(index);
            state |= FETCHED_LABEL;
        }
        return label;
    }

    public int type() throws SQLException {
        if ((state & FETCHED_TYPE) == 0) {
            type = metadata.getColumnType(index);
            state |= FETCHED_TYPE;
        }
        return type;
    }

    public @NotNull String typeName() throws SQLException {
        if ((state & FETCHED_TYPE_NAME) == 0) {
            typeName = metadata.getColumnTypeName(index);
            state |= FETCHED_TYPE_NAME;
        }
        return typeName;
    }

    public int precision() throws SQLException {
        if ((state & FETCHED_PRECISION) == 0) {
            precision = metadata.getPrecision(index);
            state |= FETCHED_PRECISION;
        }
        return precision;
    }

    public int scale() throws SQLException {
        if ((state & FETCHED_SCALE) == 0) {
            scale = metadata.getScale(index);
            state |= FETCHED_SCALE;
        }
        return scale;
    }

    public @NotNull String table() throws SQLException {
        if ((state & FETCHED_TABLE) == 0) {
            table = metadata.getTableName(index);
            state |= FETCHED_TABLE;
        }
        return table;
    }

    public @NotNull String schema() throws SQLException {
        if ((state & FETCHED_SCHEMA) == 0) {
            schema = metadata.getSchemaName(index);
            state |= FETCHED_SCHEMA;
        }
        return schema;
    }

    public @NotNull String catalog() throws SQLException {
        if ((state & FETCHED_CATALOG) == 0) {
            catalog = metadata.getCatalogName(index);
            state |= FETCHED_CATALOG;
        }
        return catalog;
    }

    public int nullable() throws SQLException {
        if ((state & FETCHED_NULLABLE) == 0) {
            nullable = metadata.isNullable(index);
            state |= FETCHED_NULLABLE;
        }
        return nullable;
    }

    public boolean signed() throws SQLException {
        int s = state;
        if ((s & FETCHED_SIGNED) == 0) {
            boolean value = metadata.isSigned(index);
            state = s | FETCHED_SIGNED | (value ? VALUE_SIGNED : 0);
            return value;
        }
        return (s & VALUE_SIGNED) != 0;
    }

    public boolean autoIncrement() throws SQLException {
        int s = state;
        if ((s & FETCHED_AUTO_INCREMENT) == 0) {
            boolean value = metadata.isAutoIncrement(index);
            state = s | FETCHED_AUTO_INCREMENT | (value ? VALUE_AUTO_INCREMENT : 0);
            return value;
        }
        return (s & VALUE_AUTO_INCREMENT) != 0;
    }

    public boolean caseSensitive() throws SQLException {
        int s = state;
        if ((s & FETCHED_CASE_SENSITIVE) == 0) {
            boolean value = metadata.isCaseSensitive(index);
            state = s | FETCHED_CASE_SENSITIVE | (value ? VALUE_CASE_SENSITIVE : 0);
            return value;
        }
        return (s & VALUE_CASE_SENSITIVE) != 0;
    }

    public boolean searchable() throws SQLException {
        int s = state;
        if ((s & FETCHED_SEARCHABLE) == 0) {
            boolean value = metadata.isSearchable(index);
            state = s | FETCHED_SEARCHABLE | (value ? VALUE_SEARCHABLE : 0);
            return value;
        }
        return (s & VALUE_SEARCHABLE) != 0;
    }

    public boolean readOnly() throws SQLException {
        int s = state;
        if ((s & FETCHED_READ_ONLY) == 0) {
            boolean value = metadata.isReadOnly(index);
            state = s | FETCHED_READ_ONLY | (value ? VALUE_READ_ONLY : 0);
            return value;
        }
        return (s & VALUE_READ_ONLY) != 0;
    }

    public boolean writable() throws SQLException {
        int s = state;
        if ((s & FETCHED_WRITABLE) == 0) {
            boolean value = metadata.isWritable(index);
            state = s | FETCHED_WRITABLE | (value ? VALUE_WRITABLE : 0);
            return value;
        }
        return (s & VALUE_WRITABLE) != 0;
    }

    public boolean definitelyWritable() throws SQLException {
        int s = state;
        if ((s & FETCHED_DEFINITELY_WRITABLE) == 0) {
            boolean value = metadata.isDefinitelyWritable(index);
            state = s | FETCHED_DEFINITELY_WRITABLE | (value ? VALUE_DEFINITELY_WRITABLE : 0);
            return value;
        }
        return (s & VALUE_DEFINITELY_WRITABLE) != 0;
    }

    public int displaySize() throws SQLException {
        if ((state & FETCHED_DISPLAY_SIZE) == 0) {
            displaySize = metadata.getColumnDisplaySize(index);
            state |= FETCHED_DISPLAY_SIZE;
        }
        return displaySize;
    }
}
