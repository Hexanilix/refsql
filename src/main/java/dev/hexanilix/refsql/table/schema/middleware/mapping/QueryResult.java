package dev.hexanilix.refsql.table.schema.middleware.mapping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class QueryResult implements ResultSet {

    private final ResultSet rs;
    private final HashMap<String, Object> map;

    private ColumnMapping<?>[][] cachedMappingKeys;
    private ResultSetColumnMetadata[][] cachedMappingValues;
    private int cachedMappingCount;


    public QueryResult(@NotNull ResultSet rs) throws SQLException {
        this.rs = rs;
        ResultSetMetaData metadata = rs.getMetaData();
        int columnCount = metadata.getColumnCount();
        this.map = HashMap.newHashMap(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            String name = metadata.getColumnLabel(i);
            var meta = new ResultSetColumnMetadata(metadata, i, name);

            Object existing = map.putIfAbsent(name, meta);
            if (existing != null) {
                if (existing instanceof ResultSetColumnMetadata first) {
                    map.put(name, new ResultSetColumnMetadata[]{ first, meta });
                } else {
                    var arr = (ResultSetColumnMetadata[]) existing;
                    var grown = Arrays.copyOf(arr, arr.length + 1);
                    grown[arr.length] = meta;
                    map.put(name, grown);
                }
            }
        }
    }

    public HashMap<String, Object> getColumnMappings() {
        return map;
    }

    public @Nullable ResultSetColumnMetadata getColumnMeta(@NotNull String name, @NotNull String table) throws SQLException {
        Object entry = map.get(name);
        if (entry == null) return null;
        if (entry instanceof ResultSetColumnMetadata meta) return meta;
        for (ResultSetColumnMetadata meta : (ResultSetColumnMetadata[]) entry)
            if (table.equals(meta.table()))
                return meta;
        return null;
    }

    public @Nullable ResultSetColumnMetadata getColumnMeta(@NotNull String name) throws SQLException {
        Object entry = map.get(name);
        if (entry == null) return null;
        if (entry instanceof ResultSetColumnMetadata meta) return meta;
        return null;
    }

    public @Nullable ResultSetColumnMetadata getColumnMeta(@NotNull ColumnMapping<?> mapping) throws SQLException {
        return getColumnMeta(mapping.column().getName(), mapping.tableName());
    }

    public @NotNull ResultSetColumnMetadata[] computeFor(ColumnMapping<?> @NotNull [] mappings) throws SQLException {
        ResultSetColumnMetadata[] metas = new ResultSetColumnMetadata[mappings.length];
        for (int i = 0; i < mappings.length; i++)
            metas[i] = getColumnMeta(mappings[i]);
        return metas;
    }

    /**
     * Resolves and caches column metadata for {@code mappings} by array identity ({@code ==})
     * so repeated row conversions on this {@code QueryResult} reuse the same resolved array.
     */
    public @NotNull ResultSetColumnMetadata[] getOrCompute(ColumnMapping<?> @NotNull [] mappings) throws SQLException {
        if (cachedMappingKeys != null) {
            for (int i = 0; i < cachedMappingCount; i++) {
                if (cachedMappingKeys[i] == mappings) {
                    return cachedMappingValues[i];
                }
            }
        }

        ResultSetColumnMetadata[] metas = computeFor(mappings);

        if (cachedMappingKeys == null) {
            cachedMappingKeys = new ColumnMapping<?>[2][];
            cachedMappingValues = new ResultSetColumnMetadata[2][];
        } else if (cachedMappingCount == cachedMappingKeys.length) {
            int newCapacity = cachedMappingKeys.length * 2;
            cachedMappingKeys = Arrays.copyOf(cachedMappingKeys, newCapacity);
            cachedMappingValues = Arrays.copyOf(cachedMappingValues, newCapacity);
        }

        cachedMappingKeys[cachedMappingCount] = mappings;
        cachedMappingValues[cachedMappingCount] = metas;
        cachedMappingCount++;

        return metas;
    }


    @Override
    public String toString() {
        return map.toString();
    }


    // ResultSet delegations
    @Override
    public boolean next() throws SQLException {
        return rs.next();
    }

    @Override
    public void close() throws SQLException {
        rs.close();
    }

    @Override
    public boolean wasNull() throws SQLException {
        return rs.wasNull();
    }

    @Override
    public String getString(int i) throws SQLException {
        return rs.getString(i);
    }

    @Override
    public boolean getBoolean(int i) throws SQLException {
        return rs.getBoolean(i);
    }

    @Override
    public byte getByte(int i) throws SQLException {
        return rs.getByte(i);
    }

    @Override
    public short getShort(int i) throws SQLException {
        return rs.getShort(i);
    }

    @Override
    public int getInt(int i) throws SQLException {
        return rs.getInt(i);
    }

    @Override
    public long getLong(int i) throws SQLException {
        return rs.getLong(i);
    }

    @Override
    public float getFloat(int i) throws SQLException {
        return rs.getFloat(i);
    }

    @Override
    public double getDouble(int i) throws SQLException {
        return rs.getDouble(i);
    }

    @Deprecated(since = "1.2")
    @Override
    public BigDecimal getBigDecimal(int i, int i1) throws SQLException {
        return rs.getBigDecimal(i, i1);
    }

    @Override
    public byte[] getBytes(int i) throws SQLException {
        return rs.getBytes(i);
    }

    @Override
    public Date getDate(int i) throws SQLException {
        return rs.getDate(i);
    }

    @Override
    public Time getTime(int i) throws SQLException {
        return rs.getTime(i);
    }

    @Override
    public Timestamp getTimestamp(int i) throws SQLException {
        return rs.getTimestamp(i);
    }

    @Override
    public InputStream getAsciiStream(int i) throws SQLException {
        return rs.getAsciiStream(i);
    }

    @Deprecated(since = "1.2")
    @Override
    public InputStream getUnicodeStream(int i) throws SQLException {
        return rs.getUnicodeStream(i);
    }

    @Override
    public InputStream getBinaryStream(int i) throws SQLException {
        return rs.getBinaryStream(i);
    }

    @Override
    public String getString(String s) throws SQLException {
        return rs.getString(s);
    }

    @Override
    public boolean getBoolean(String s) throws SQLException {
        return rs.getBoolean(s);
    }

    @Override
    public byte getByte(String s) throws SQLException {
        return rs.getByte(s);
    }

    @Override
    public short getShort(String s) throws SQLException {
        return rs.getShort(s);
    }

    @Override
    public int getInt(String s) throws SQLException {
        return rs.getInt(s);
    }

    @Override
    public long getLong(String s) throws SQLException {
        return rs.getLong(s);
    }

    @Override
    public float getFloat(String s) throws SQLException {
        return rs.getFloat(s);
    }

    @Override
    public double getDouble(String s) throws SQLException {
        return rs.getDouble(s);
    }

    @Deprecated(since = "1.2")
    @Override
    public BigDecimal getBigDecimal(String s, int i) throws SQLException {
        return rs.getBigDecimal(s, i);
    }

    @Override
    public byte[] getBytes(String s) throws SQLException {
        return rs.getBytes(s);
    }

    @Override
    public Date getDate(String s) throws SQLException {
        return rs.getDate(s);
    }

    @Override
    public Time getTime(String s) throws SQLException {
        return rs.getTime(s);
    }

    @Override
    public Timestamp getTimestamp(String s) throws SQLException {
        return rs.getTimestamp(s);
    }

    @Override
    public InputStream getAsciiStream(String s) throws SQLException {
        return rs.getAsciiStream(s);
    }

    @Deprecated(since = "1.2")
    @Override
    public InputStream getUnicodeStream(String s) throws SQLException {
        return rs.getUnicodeStream(s);
    }

    @Override
    public InputStream getBinaryStream(String s) throws SQLException {
        return rs.getBinaryStream(s);
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return rs.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        rs.clearWarnings();
    }

    @Override
    public String getCursorName() throws SQLException {
        return rs.getCursorName();
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return rs.getMetaData();
    }

    @Override
    public Object getObject(int i) throws SQLException {
        return rs.getObject(i);
    }

    @Override
    public Object getObject(String s) throws SQLException {
        return rs.getObject(s);
    }

    @Override
    public int findColumn(String s) throws SQLException {
        return rs.findColumn(s);
    }

    @Override
    public Reader getCharacterStream(int i) throws SQLException {
        return rs.getCharacterStream(i);
    }

    @Override
    public Reader getCharacterStream(String s) throws SQLException {
        return rs.getCharacterStream(s);
    }

    @Override
    public BigDecimal getBigDecimal(int i) throws SQLException {
        return rs.getBigDecimal(i);
    }

    @Override
    public BigDecimal getBigDecimal(String s) throws SQLException {
        return rs.getBigDecimal(s);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return rs.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return rs.isAfterLast();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return rs.isFirst();
    }

    @Override
    public boolean isLast() throws SQLException {
        return rs.isLast();
    }

    @Override
    public void beforeFirst() throws SQLException {
        rs.beforeFirst();
    }

    @Override
    public void afterLast() throws SQLException {
        rs.afterLast();
    }

    @Override
    public boolean first() throws SQLException {
        return rs.first();
    }

    @Override
    public boolean last() throws SQLException {
        return rs.last();
    }

    @Override
    public int getRow() throws SQLException {
        return rs.getRow();
    }

    @Override
    public boolean absolute(int i) throws SQLException {
        return rs.absolute(i);
    }

    @Override
    public boolean relative(int i) throws SQLException {
        return rs.relative(i);
    }

    @Override
    public boolean previous() throws SQLException {
        return rs.previous();
    }

    @Override
    public void setFetchDirection(int i) throws SQLException {
        rs.setFetchDirection(i);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return rs.getFetchDirection();
    }

    @Override
    public void setFetchSize(int i) throws SQLException {
        rs.setFetchSize(i);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return rs.getFetchSize();
    }

    @Override
    public int getType() throws SQLException {
        return rs.getType();
    }

    @Override
    public int getConcurrency() throws SQLException {
        return rs.getConcurrency();
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return rs.rowUpdated();
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return rs.rowInserted();
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return rs.rowDeleted();
    }

    @Override
    public void updateNull(int i) throws SQLException {
        rs.updateNull(i);
    }

    @Override
    public void updateBoolean(int i, boolean b) throws SQLException {
        rs.updateBoolean(i, b);
    }

    @Override
    public void updateByte(int i, byte b) throws SQLException {
        rs.updateByte(i, b);
    }

    @Override
    public void updateShort(int i, short i1) throws SQLException {
        rs.updateShort(i, i1);
    }

    @Override
    public void updateInt(int i, int i1) throws SQLException {
        rs.updateInt(i, i1);
    }

    @Override
    public void updateLong(int i, long l) throws SQLException {
        rs.updateLong(i, l);
    }

    @Override
    public void updateFloat(int i, float v) throws SQLException {
        rs.updateFloat(i, v);
    }

    @Override
    public void updateDouble(int i, double v) throws SQLException {
        rs.updateDouble(i, v);
    }

    @Override
    public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
        rs.updateBigDecimal(i, bigDecimal);
    }

    @Override
    public void updateString(int i, String s) throws SQLException {
        rs.updateString(i, s);
    }

    @Override
    public void updateBytes(int i, byte[] bytes) throws SQLException {
        rs.updateBytes(i, bytes);
    }

    @Override
    public void updateDate(int i, Date date) throws SQLException {
        rs.updateDate(i, date);
    }

    @Override
    public void updateTime(int i, Time time) throws SQLException {
        rs.updateTime(i, time);
    }

    @Override
    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
        rs.updateTimestamp(i, timestamp);
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {
        rs.updateAsciiStream(i, inputStream, i1);
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {
        rs.updateBinaryStream(i, inputStream, i1);
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, int i1) throws SQLException {
        rs.updateCharacterStream(i, reader, i1);
    }

    @Override
    public void updateObject(int i, Object o, int i1) throws SQLException {
        rs.updateObject(i, o, i1);
    }

    @Override
    public void updateObject(int i, Object o) throws SQLException {
        rs.updateObject(i, o);
    }

    @Override
    public void updateNull(String s) throws SQLException {
        rs.updateNull(s);
    }

    @Override
    public void updateBoolean(String s, boolean b) throws SQLException {
        rs.updateBoolean(s, b);
    }

    @Override
    public void updateByte(String s, byte b) throws SQLException {
        rs.updateByte(s, b);
    }

    @Override
    public void updateShort(String s, short i) throws SQLException {
        rs.updateShort(s, i);
    }

    @Override
    public void updateInt(String s, int i) throws SQLException {
        rs.updateInt(s, i);
    }

    @Override
    public void updateLong(String s, long l) throws SQLException {
        rs.updateLong(s, l);
    }

    @Override
    public void updateFloat(String s, float v) throws SQLException {
        rs.updateFloat(s, v);
    }

    @Override
    public void updateDouble(String s, double v) throws SQLException {
        rs.updateDouble(s, v);
    }

    @Override
    public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException {
        rs.updateBigDecimal(s, bigDecimal);
    }

    @Override
    public void updateString(String s, String s1) throws SQLException {
        rs.updateString(s, s1);
    }

    @Override
    public void updateBytes(String s, byte[] bytes) throws SQLException {
        rs.updateBytes(s, bytes);
    }

    @Override
    public void updateDate(String s, Date date) throws SQLException {
        rs.updateDate(s, date);
    }

    @Override
    public void updateTime(String s, Time time) throws SQLException {
        rs.updateTime(s, time);
    }

    @Override
    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
        rs.updateTimestamp(s, timestamp);
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException {
        rs.updateAsciiStream(s, inputStream, i);
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException {
        rs.updateBinaryStream(s, inputStream, i);
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
        rs.updateCharacterStream(s, reader, i);
    }

    @Override
    public void updateObject(String s, Object o, int i) throws SQLException {
        rs.updateObject(s, o, i);
    }

    @Override
    public void updateObject(String s, Object o) throws SQLException {
        rs.updateObject(s, o);
    }

    @Override
    public void insertRow() throws SQLException {
        rs.insertRow();
    }

    @Override
    public void updateRow() throws SQLException {
        rs.updateRow();
    }

    @Override
    public void deleteRow() throws SQLException {
        rs.deleteRow();
    }

    @Override
    public void refreshRow() throws SQLException {
        rs.refreshRow();
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        rs.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        rs.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        rs.moveToCurrentRow();
    }

    @Override
    public Statement getStatement() throws SQLException {
        return rs.getStatement();
    }

    @Override
    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
        return rs.getObject(i, map);
    }

    @Override
    public Ref getRef(int i) throws SQLException {
        return rs.getRef(i);
    }

    @Override
    public Blob getBlob(int i) throws SQLException {
        return rs.getBlob(i);
    }

    @Override
    public Clob getClob(int i) throws SQLException {
        return rs.getClob(i);
    }

    @Override
    public Array getArray(int i) throws SQLException {
        return rs.getArray(i);
    }

    @Override
    public Object getObject(String s, Map<String, Class<?>> map) throws SQLException {
        return rs.getObject(s, map);
    }

    @Override
    public Ref getRef(String s) throws SQLException {
        return rs.getRef(s);
    }

    @Override
    public Blob getBlob(String s) throws SQLException {
        return rs.getBlob(s);
    }

    @Override
    public Clob getClob(String s) throws SQLException {
        return rs.getClob(s);
    }

    @Override
    public Array getArray(String s) throws SQLException {
        return rs.getArray(s);
    }

    @Override
    public Date getDate(int i, Calendar calendar) throws SQLException {
        return rs.getDate(i, calendar);
    }

    @Override
    public Date getDate(String s, Calendar calendar) throws SQLException {
        return rs.getDate(s, calendar);
    }

    @Override
    public Time getTime(int i, Calendar calendar) throws SQLException {
        return rs.getTime(i, calendar);
    }

    @Override
    public Time getTime(String s, Calendar calendar) throws SQLException {
        return rs.getTime(s, calendar);
    }

    @Override
    public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
        return rs.getTimestamp(i, calendar);
    }

    @Override
    public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
        return rs.getTimestamp(s, calendar);
    }

    @Override
    public URL getURL(int i) throws SQLException {
        return rs.getURL(i);
    }

    @Override
    public URL getURL(String s) throws SQLException {
        return rs.getURL(s);
    }

    @Override
    public void updateRef(int i, Ref ref) throws SQLException {
        rs.updateRef(i, ref);
    }

    @Override
    public void updateRef(String s, Ref ref) throws SQLException {
        rs.updateRef(s, ref);
    }

    @Override
    public void updateBlob(int i, Blob blob) throws SQLException {
        rs.updateBlob(i, blob);
    }

    @Override
    public void updateBlob(String s, Blob blob) throws SQLException {
        rs.updateBlob(s, blob);
    }

    @Override
    public void updateClob(int i, Clob clob) throws SQLException {
        rs.updateClob(i, clob);
    }

    @Override
    public void updateClob(String s, Clob clob) throws SQLException {
        rs.updateClob(s, clob);
    }

    @Override
    public void updateArray(int i, Array array) throws SQLException {
        rs.updateArray(i, array);
    }

    @Override
    public void updateArray(String s, Array array) throws SQLException {
        rs.updateArray(s, array);
    }

    @Override
    public RowId getRowId(int i) throws SQLException {
        return rs.getRowId(i);
    }

    @Override
    public RowId getRowId(String s) throws SQLException {
        return rs.getRowId(s);
    }

    @Override
    public void updateRowId(int i, RowId rowId) throws SQLException {
        rs.updateRowId(i, rowId);
    }

    @Override
    public void updateRowId(String s, RowId rowId) throws SQLException {
        rs.updateRowId(s, rowId);
    }

    @Override
    public int getHoldability() throws SQLException {
        return rs.getHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return rs.isClosed();
    }

    @Override
    public void updateNString(int i, String s) throws SQLException {
        rs.updateNString(i, s);
    }

    @Override
    public void updateNString(String s, String s1) throws SQLException {
        rs.updateNString(s, s1);
    }

    @Override
    public void updateNClob(int i, NClob nClob) throws SQLException {
        rs.updateNClob(i, nClob);
    }

    @Override
    public void updateNClob(String s, NClob nClob) throws SQLException {
        rs.updateNClob(s, nClob);
    }

    @Override
    public NClob getNClob(int i) throws SQLException {
        return rs.getNClob(i);
    }

    @Override
    public NClob getNClob(String s) throws SQLException {
        return rs.getNClob(s);
    }

    @Override
    public SQLXML getSQLXML(int i) throws SQLException {
        return rs.getSQLXML(i);
    }

    @Override
    public SQLXML getSQLXML(String s) throws SQLException {
        return rs.getSQLXML(s);
    }

    @Override
    public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException {
        rs.updateSQLXML(i, sqlxml);
    }

    @Override
    public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException {
        rs.updateSQLXML(s, sqlxml);
    }

    @Override
    public String getNString(int i) throws SQLException {
        return rs.getNString(i);
    }

    @Override
    public String getNString(String s) throws SQLException {
        return rs.getNString(s);
    }

    @Override
    public Reader getNCharacterStream(int i) throws SQLException {
        return rs.getNCharacterStream(i);
    }

    @Override
    public Reader getNCharacterStream(String s) throws SQLException {
        return rs.getNCharacterStream(s);
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader, long l) throws SQLException {
        rs.updateNCharacterStream(i, reader, l);
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader, long l) throws SQLException {
        rs.updateNCharacterStream(s, reader, l);
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, long l) throws SQLException {
        rs.updateAsciiStream(i, inputStream, l);
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, long l) throws SQLException {
        rs.updateBinaryStream(i, inputStream, l);
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, long l) throws SQLException {
        rs.updateCharacterStream(i, reader, l);
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, long l) throws SQLException {
        rs.updateAsciiStream(s, inputStream, l);
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, long l) throws SQLException {
        rs.updateBinaryStream(s, inputStream, l);
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, long l) throws SQLException {
        rs.updateCharacterStream(s, reader, l);
    }

    @Override
    public void updateBlob(int i, InputStream inputStream, long l) throws SQLException {
        rs.updateBlob(i, inputStream, l);
    }

    @Override
    public void updateBlob(String s, InputStream inputStream, long l) throws SQLException {
        rs.updateBlob(s, inputStream, l);
    }

    @Override
    public void updateClob(int i, Reader reader, long l) throws SQLException {
        rs.updateClob(i, reader, l);
    }

    @Override
    public void updateClob(String s, Reader reader, long l) throws SQLException {
        rs.updateClob(s, reader, l);
    }

    @Override
    public void updateNClob(int i, Reader reader, long l) throws SQLException {
        rs.updateNClob(i, reader, l);
    }

    @Override
    public void updateNClob(String s, Reader reader, long l) throws SQLException {
        rs.updateNClob(s, reader, l);
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader) throws SQLException {
        rs.updateNCharacterStream(i, reader);
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader) throws SQLException {
        rs.updateNCharacterStream(s, reader);
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream) throws SQLException {
        rs.updateAsciiStream(i, inputStream);
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream) throws SQLException {
        rs.updateBinaryStream(i, inputStream);
    }

    @Override
    public void updateCharacterStream(int i, Reader reader) throws SQLException {
        rs.updateCharacterStream(i, reader);
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream) throws SQLException {
        rs.updateAsciiStream(s, inputStream);
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream) throws SQLException {
        rs.updateBinaryStream(s, inputStream);
    }

    @Override
    public void updateCharacterStream(String s, Reader reader) throws SQLException {
        rs.updateCharacterStream(s, reader);
    }

    @Override
    public void updateBlob(int i, InputStream inputStream) throws SQLException {
        rs.updateBlob(i, inputStream);
    }

    @Override
    public void updateBlob(String s, InputStream inputStream) throws SQLException {
        rs.updateBlob(s, inputStream);
    }

    @Override
    public void updateClob(int i, Reader reader) throws SQLException {
        rs.updateClob(i, reader);
    }

    @Override
    public void updateClob(String s, Reader reader) throws SQLException {
        rs.updateClob(s, reader);
    }

    @Override
    public void updateNClob(int i, Reader reader) throws SQLException {
        rs.updateNClob(i, reader);
    }

    @Override
    public void updateNClob(String s, Reader reader) throws SQLException {
        rs.updateNClob(s, reader);
    }

    @Override
    public <T> T getObject(int i, Class<T> aClass) throws SQLException {
        return rs.getObject(i, aClass);
    }

    @Override
    public <T> T getObject(String s, Class<T> aClass) throws SQLException {
        return rs.getObject(s, aClass);
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        rs.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
        rs.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
        rs.updateObject(columnIndex, x, targetSqlType);
    }

    @Override
    public void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
        rs.updateObject(columnLabel, x, targetSqlType);
    }

    @Override
    public <T> T unwrap(Class<T> aClass) throws SQLException {
        return rs.unwrap(aClass);
    }

    @Override
    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return rs.isWrapperFor(aClass);
    }
}
