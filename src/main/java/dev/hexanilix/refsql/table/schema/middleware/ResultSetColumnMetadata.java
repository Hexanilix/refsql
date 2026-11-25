package dev.hexanilix.refsql.table.schema.middleware;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public interface ResultSetColumnMetadata {

    @NotNull String name() throws SQLException;
    @NotNull String label() throws SQLException;
    int index();

    int type() throws SQLException;
    @NotNull String typeName() throws SQLException;

    int precision() throws SQLException;
    int scale() throws SQLException;

    @NotNull String table() throws SQLException;
    @NotNull String schema() throws SQLException;
    @NotNull String catalog() throws SQLException;

    int nullable() throws SQLException;

    boolean signed() throws SQLException;
    boolean autoIncrement() throws SQLException;
    boolean caseSensitive() throws SQLException;
    boolean searchable() throws SQLException;

    boolean readOnly() throws SQLException;
    boolean writable() throws SQLException;
    boolean definitelyWritable() throws SQLException;

    int displaySize() throws SQLException;

    static Map<String, ResultSetColumnMetadata> buildMappings(
            @NotNull ResultSet rs
    ) throws SQLException {

        Map<String, ResultSetColumnMetadata> map = new LinkedHashMap<>();
        ResultSetMetaData metadata = rs.getMetaData();

        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            final int index = i;

            map.put(metadata.getColumnLabel(i), new ResultSetColumnMetadata() {
                @Override public @NotNull String name() throws SQLException {
                    return metadata.getColumnName(index);
                }
                @Override public @NotNull String label() throws SQLException {
                    return metadata.getColumnLabel(index);
                }
                @Override public int index() { return index; }
                @Override public int type() throws SQLException {
                    return metadata.getColumnType(index);
                }
                @Override public @NotNull String typeName() throws SQLException {
                    return metadata.getColumnTypeName(index);
                }
                @Override public int precision() throws SQLException {
                    return metadata.getPrecision(index);
                }
                @Override public int scale() throws SQLException {
                    return metadata.getScale(index);
                }
                @Override public @NotNull String table() throws SQLException {
                    return metadata.getTableName(index);
                }
                @Override public @NotNull String schema() throws SQLException {
                    return metadata.getSchemaName(index);
                }
                @Override public @NotNull String catalog() throws SQLException {
                    return metadata.getCatalogName(index);
                }
                @Override public int nullable() throws SQLException {
                    return metadata.isNullable(index);
                }
                @Override public boolean signed() throws SQLException {
                    return metadata.isSigned(index);
                }
                @Override public boolean autoIncrement() throws SQLException {
                    return metadata.isAutoIncrement(index);
                }
                @Override public boolean caseSensitive() throws SQLException {
                    return metadata.isCaseSensitive(index);
                }
                @Override public boolean searchable() throws SQLException {
                    return metadata.isSearchable(index);
                }
                @Override public boolean readOnly() throws SQLException {
                    return metadata.isReadOnly(index);
                }
                @Override public boolean writable() throws SQLException {
                    return metadata.isWritable(index);
                }
                @Override public boolean definitelyWritable() throws SQLException {
                    return metadata.isDefinitelyWritable(index);
                }
                @Override public int displaySize() throws SQLException {
                    return metadata.getColumnDisplaySize(index);
                }
            });
        }

        return map;
    }
}
