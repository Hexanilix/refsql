package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.SimpleConvertingFunction;
import dev.hexanilix.refsql.SqlErrorHandler;
import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.exceptions.SqlExecutionError;
import dev.hexanilix.refsql.queries.Upsert;
import dev.hexanilix.refsql.queries.select.SelectModifiers;
import dev.hexanilix.refsql.repo.cache.CompositeKey;
import dev.hexanilix.refsql.repo.cache.TableSnapshot;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.TableIndex;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.middleware.ResultSetMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

public class BaseTableRepository<Schema extends BaseTableSchema<?>> {

    public Array mapArray(@NotNull Collection<?> objects, @NotNull SqlType type) {
        try {
            Object[] arr = objects.toArray();
            for (int i = 0; i < arr.length; i++)
                arr[i] = SqlType.toSqlValue(arr[i], schema.getTypeRelationManager());

            return db.createArrayOf(type.name(), arr);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(null, e);
        }
    }

    public static @NotNull String formatQuery(@NotNull String sql, @NotNull BaseTableSchema<?> schema) {
        return sql.replace(":name", schema.getName());
    }

    @Contract("_, _ -> param1")
    public static @NotNull PreparedStatement setValues(@NotNull PreparedStatement ps, @Nullable Collection<?> values) throws SQLException {
        if (values == null) return ps;
        int i = 1;
        for (Object value : values)
            ps.setObject(i++, value);
        return ps;
    }

    public static <T> @NotNull ArrayList<T> readRsAsList(@NotNull ResultSet rs, ConvertingFunction<T> func) throws SQLException {
        ArrayList<T> list = new ArrayList<>();
        if (rs.next()) {
            if (func instanceof SimpleConvertingFunction<T> simple) {
                do {
                    list.add(simple.apply(rs));
                } while (rs.next());
            } else {
                ResultSetMap map = new ResultSetMap(rs);
                do {
                    list.add(func.apply(rs, map));
                } while (rs.next());
            }
        }
        return list;
    }
    public static <T> @NotNull Optional<T> readRsAsOne(@NotNull ResultSet rs, ConvertingFunction<T> func) throws SQLException {
        if (!rs.next()) return Optional.empty();
        if (func instanceof SimpleConvertingFunction<T> simple) {
            return Optional.ofNullable(simple.apply(rs));
        }
        return Optional.ofNullable(func.apply(rs, new ResultSetMap(rs)));
    }

    protected final Connection db;
    protected final Schema schema;

    // Queries
    protected final String create_table_if_not_exists_query;
    protected final String count_query;
    protected final String select_all_query;
    protected final String upsert_query;
    protected final String exists_by_primary_key_query;
    protected final String get_by_primary_key_query;
    protected final String delete_by_primary_key_query;
    public BaseTableRepository(@NotNull Schema schema, Connection db) {
        this.schema = schema;
        this.db = db;

        this.create_table_if_not_exists_query = schema.createTableIfNotExistsSql();

        this.count_query = "SELECT COUNT(*) FROM " + schema.getName();
        this.select_all_query = "SELECT * FROM " + schema.getName();

        Upsert.Builder upsert_builder = Upsert.Builder.n(schema.getName()).columns(schema.getColumns());
        if (schema.hasPrimaryKey()) {
            upsert_builder
                    .onConflict(
                            Upsert.ConfictUpdateSetBuilder.n(schema.getColumns(), schema.getPrimaryKeys())
                                    .build()
                    );
        }
        this.upsert_query = upsert_builder.build();

        if (schema.hasPrimaryKey()) {
            StringBuilder sb = new StringBuilder("FROM " + schema.getName() + " WHERE ");
            boolean next = false;
            for (TableColumn key : schema.getPrimaryKeys()) {
                if (next) sb.append(" AND ");
                sb.append(key.getName()).append(" = ?");
                next = true;
            }
            this.exists_by_primary_key_query = "SELECT EXISTS (SELECT 1 " + sb + ")";
            this.get_by_primary_key_query = "SELECT * " + sb;
            this.delete_by_primary_key_query = "DELETE " + sb;
        } else {
            this.exists_by_primary_key_query = null;
            this.get_by_primary_key_query = null;
            this.delete_by_primary_key_query = null;
        }
    }

    public Schema getSchema() {
        return schema;
    }

    public void ensureTableExists() {
        ensureTableExists(false, true);
    }

    public void ensureTableExists(boolean transactional, boolean indexes) {
        boolean previous_auto_commit;

        try {
            previous_auto_commit = db.getAutoCommit();
            if (transactional)
                db.setAutoCommit(false);

            String currentQuery = this.create_table_if_not_exists_query;
            try (Statement s = db.createStatement()) {
                try {
                    s.executeUpdate(currentQuery);

                    if (schema.hasIndexes() && indexes) {
                        for (TableIndex index : schema.getIndexes()) {
                            currentQuery = index.buildSql(schema);
                            s.executeUpdate(currentQuery);
                        }
                    }

                    if (transactional)
                        db.commit();
                } catch (SQLException e) {
                    if (transactional)
                        db.rollback();
                    throw new SqlExecutionError(currentQuery, e);
                }
            } finally {
                if (transactional)
                    db.setAutoCommit(previous_auto_commit);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public long count() {
        try (Statement statement = db.createStatement();
             ResultSet rs = statement.executeQuery(count_query)) {
            if (!rs.next())
                throw new IllegalStateException("No rows returned in count query for table " + schema.getName());
            return rs.getLong(1);
        } catch (SQLException e) { throw new SqlExecutionError(count_query, e); }
    }

    public <T> List<T> getAll(SimpleConvertingFunction<T> func) throws SqlExecutionError { return this.getAll((ConvertingFunction<T>) func); }
    public <T> List<T> getAll(ConvertingFunction<T> func) throws SqlExecutionError {
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(select_all_query)) {
            return readRsAsList(set, func);
        } catch (SQLException e) {
            throw new SqlExecutionError(select_all_query, e);
        }
    }

    public <T> List<T> getAll(@NotNull SelectModifiers modifs, SimpleConvertingFunction<T> func) throws SqlExecutionError { return this.getAll(modifs, (ConvertingFunction<T>) func); }
    public <T> List<T> getAll(@NotNull SelectModifiers modifs, ConvertingFunction<T> func) throws SqlExecutionError {
        String query = select_all_query + " " + modifs.build(schema.getTypeRelationManager());
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(query)) {
            return readRsAsList(set, func);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }

    public <T> List<T> getList(String query, SimpleConvertingFunction<T> func, Object... params) throws SqlExecutionError { return this.getList(query, (ConvertingFunction<T>) func, params); }
    public <T> List<T> getList(String query, ConvertingFunction<T> func, Object... params) throws SqlExecutionError {
        return getList(query, func, Arrays.asList(params));
    }

    public <T> List<T> getList(String query, SimpleConvertingFunction<T> func, @Nullable Collection<?> params) throws SqlExecutionError { return this.getList(query, (ConvertingFunction<T>) func, params); }
    public <T> List<T> getList(String query, ConvertingFunction<T> func, @Nullable Collection<?> params) throws SqlExecutionError {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()));
             ResultSet rs = setValues(ps, params).executeQuery()) {
            return readRsAsList(rs, func);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }

    public <T> Optional<T> getOne(String query, SimpleConvertingFunction<T> func, Object ... params) throws SqlExecutionError { return this.getOne(query, (ConvertingFunction<T>) func, params); }
    public <T> Optional<T> getOne(String query, ConvertingFunction<T> func, Object ... params) throws SqlExecutionError {
        return getOne(query, func, Arrays.asList(params));
    }

    public <T> Optional<T> getOne(String query, SimpleConvertingFunction<T> func, Collection<?> params) throws SqlExecutionError { return this.getOne(query, (ConvertingFunction<T>) func, params); }
    public <T> Optional<T> getOne(String query, ConvertingFunction<T> func, Collection<?> params) throws SqlExecutionError {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()));
             ResultSet rs = setValues(ps, params).executeQuery()) {
            return readRsAsOne(rs, func);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }

    /**
     * A helper function that throws if the passed schema doesn't have a primary key
     * @param schema the schema to check
     * @throws IllegalStateException the thrown exception
     */
    protected static void ensureHashPrimaryKey(@NotNull BaseTableSchema<?> schema) throws IllegalStateException {
        if (!schema.hasPrimaryKey())
            throw new IllegalStateException("Table " + schema.getName() + " has no primary key(s), therefore requesting an entity by the primary key(s) is impossible");
    }

    private static @NotNull String writeAliases(@NotNull SqlType type) {
        StringBuilder sb = new StringBuilder("(");
        boolean comma = false;
        for (Class<?> clazz : type.getAliases()) {
            if (comma) sb.append(", ");
            sb.append(clazz.getCanonicalName());
            comma = true;
        }
        return sb.append(")").toString();
    }

    private PreparedStatement primaryKeyStatement(String query, @NotNull Collection<?> keys) throws SQLException {
        ensureHashPrimaryKey(schema);
        List<? extends TableColumn> pk_columns = schema.getPrimaryKeys();

        if (keys.size() != pk_columns.size())
            throw new IllegalArgumentException("Number of provided primary keys does not match number of primary keys in the table: Expected "
                    + pk_columns.size() + " but got " + keys.size());

        int param = 1;
        PreparedStatement s = db.prepareStatement(query);
        try {
            for (Object key : keys) {
                Object value = schema.hasTypeRelationManager()
                        ? schema.getTypeRelationManager().convert(key)
                        : key;

                if (value == null)
                    throw new IllegalArgumentException(
                            "Primary key " + param + " value is null"
                    );

                TableColumn column = pk_columns.get(param - 1);

                if (SqlType.from(value.getClass()) != column.getSqlType()) {
                    throw  new IllegalArgumentException(
                            "Invalid primary key parameter " + param
                                    + " for column '" + column.getName() + "': "
                                    + "expected " +  column.getSqlType().name()
                                    + " " + writeAliases(column.getSqlType())
                                    + ", but received " + value.getClass().getCanonicalName() + " (" + value + ")"
                    );
                }
                s.setObject(param++, value);
            }
        } catch (Exception e) {
            try { s.close(); }
            catch (SQLException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }

        return s;
    }

    public <T> Optional<T> getByPrimaryKey(SimpleConvertingFunction<T> func, Object... keys) throws SqlExecutionError { return getByPrimaryKey((ConvertingFunction<T>) func, Arrays.asList(keys)); }
    public <T> Optional<T> getByPrimaryKey(ConvertingFunction<T> func, Object... keys) throws SqlExecutionError {
        return getByPrimaryKey(func, Arrays.asList(keys));
    }

    public <T> Optional<T> getByPrimaryKey(SimpleConvertingFunction<T> func, Collection<?> keys)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException { this.getByPrimaryKey((ConvertingFunction<T>) func, keys); return Optional.empty(); }
    public <T> Optional<T> getByPrimaryKey(ConvertingFunction<T> func, Collection<?> keys)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(get_by_primary_key_query, keys)) {
            return readRsAsOne(ps.executeQuery(), func);
        }  catch (SQLException e) {
            throw SqlErrorHandler.handle(get_by_primary_key_query, e);
        }
    }

    public boolean existsByPrimaryKey(Object... keys)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        return existsByPrimaryKey(Arrays.asList(keys));
    }
    public boolean existsByPrimaryKey(Collection<?> keys)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(exists_by_primary_key_query, keys)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean(1);
        }  catch (SQLException e) {
            throw SqlErrorHandler.handle(exists_by_primary_key_query, e);
        }
    }
    public boolean existsWhere(String where, Object... values)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        return existsWhere(where, Arrays.asList(values));
    }
    public boolean existsWhere(String where, Collection<?> values)  throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        String query = formatQuery("SELECT EXISTS (SELECT 1 FROM :name WHERE " + where + ")", this.schema);
        try (PreparedStatement ps = db.prepareStatement(query)) {
            ResultSet rs = setValues(ps, values).executeQuery();
            return rs.next() && rs.getBoolean(1);
        }  catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }

    public <T> List<T> page(String where, String order_by, long offset, long limit, SimpleConvertingFunction<T> func, Object... values) throws SqlExecutionError { return this.page(where, order_by, offset, limit, (ConvertingFunction<T>) func, Arrays.asList(values)); }
    public <T> List<T> page(String where, String order_by, long offset, long limit, ConvertingFunction<T> func, Object... values) throws SqlExecutionError {
        return page(where, order_by, offset, limit, func, Arrays.asList(values));
    }
    public <T> List<T> page(String where, String order_by, long offset, long limit, SimpleConvertingFunction<T> func, @Nullable Collection<?> values) throws SqlExecutionError { return this.page(where, order_by, offset, limit, (ConvertingFunction<T>) func, values); }
    public <T> List<T> page(String where, String order_by, long offset, long limit, ConvertingFunction<T> func, @Nullable Collection<?> values) throws SqlExecutionError {
        String query = "SELECT * FROM " + this.schema.getName();
        if (where != null) query += " WHERE " + where;
        if (order_by != null) query += " ORDER BY " + order_by;
        query += " LIMIT " + limit + " OFFSET " + offset;
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()));
             ResultSet rs = setValues(ps, values).executeQuery()) {
                return readRsAsList(rs, func);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }


    public int update(String query, Object... params) throws SqlExecutionError {
        return update(query, Arrays.asList(params));
    }
    public int update(String query, @Nullable Collection<?> params) throws SqlExecutionError {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            setValues(ps, params).executeUpdate();
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }

    public boolean deleteByPrimaryKey(Object... keys) throws SqlExecutionError {
        return deleteByPrimaryKey(Arrays.asList(keys));
    }

    public boolean deleteByPrimaryKey(Collection<?> keys) throws SqlExecutionError, IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(delete_by_primary_key_query, keys)) {
            return ps.executeUpdate() != 0;
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(delete_by_primary_key_query, e);
        }
    }

    public int truncate() throws SqlExecutionError {
        String query = "TRUNCATE TABLE " + this.schema.getName();
        try (Statement s = db.createStatement()) {
            return s.executeUpdate(query);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }
    public int drop() throws SqlExecutionError {
        String query = "DROP TABLE " + schema.getName();
        try (Statement s = db.createStatement()) {
            return s.executeUpdate(query);
        } catch (SQLException e) {
            throw SqlErrorHandler.handle(query, e);
        }
    }


    public <T> TableSnapshot<T> createPrimaryKeySnapshot(ConvertingFunction<T> func) {

        ensureHashPrimaryKey(schema);

        try (Statement s = db.createStatement();
             ResultSet rs = s.executeQuery(select_all_query)) {

            HashMap<CompositeKey, T> map = new HashMap<>();

            if (rs.next()) {
                List<? extends TableColumn> primaryKeys = schema.getPrimaryKeys();
                int pkSize = primaryKeys.size();
                ResultSetMap rsMap = new ResultSetMap(rs);

                int[] pkIndices = new int[pkSize];
                for (int i = 0; i < pkSize; i++) {
                    var pkMeta = rsMap.get(primaryKeys.get(i).getName(), schema.getName());
                    pkIndices[i] = pkMeta != null ? pkMeta.index() : rs.findColumn(primaryKeys.get(i).getName());
                }

                do {
                    Object[] keys = new Object[pkSize];
                    for (int i = 0; i < pkSize; i++)
                        keys[i] = rs.getObject(pkIndices[i]);

                    T value = func.apply(rs, rsMap);
                    map.put(new CompositeKey(keys), value);
                } while (rs.next());
            }

            return new TableSnapshot<>(map);

        } catch (SQLException e) {
            throw SqlErrorHandler.handle(select_all_query, e);
        }
    }
}
