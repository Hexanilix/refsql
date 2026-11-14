package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.PostgresErrorHandler;
import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.exceptions.SqlExecutionError;
import dev.hexanilix.refsql.queries.Upsert;
import dev.hexanilix.refsql.queries.select.SelectModifiers;
import dev.hexanilix.refsql.table.TableColumn;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;

public class BaseTableRepository<Schema extends BaseTableSchema<?>> {

    public static @NotNull String formatQuery(@NotNull String sql, @NotNull BaseTableSchema<?> schema) {
        return sql.replace(":name", schema.getName());
    }

    @Contract("_, _ -> param1")
    public static PreparedStatement setValues(PreparedStatement ps, @NotNull Collection<Object> values) throws SQLException {
        int i = 1;
        for (Object value : values)
            ps.setObject(i++, value);
        return ps;
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
        try (Statement s = db.createStatement()) {
            s.executeUpdate(create_table_if_not_exists_query);
        } catch (SQLException e) { throw new SqlExecutionError(create_table_if_not_exists_query, e); }
    }

    public long count() {
        try (Statement statement = db.createStatement();
             ResultSet rs = statement.executeQuery(count_query)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) { throw new SqlExecutionError(count_query, e); }
    }

    public <T> List<T> getAll(ConvertingFunction<T> func) {
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(select_all_query)) {

            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(func.apply(set));
            return list;
        } catch (SQLException e) {
            throw new SqlExecutionError(select_all_query, e);
        }
    }

    public <T> List<T> getAll(@NotNull SelectModifiers modifs, ConvertingFunction<T> func) throws SQLException {
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(
                     select_all_query + " " + modifs.build(schema.getTypeRelationManager())
             )) {

            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(func.apply(set));
            return list;
        }
    }

    public <T> List<T> getList(String query, ConvertingFunction<T> func, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params)
                ps.setObject(i++, param);

            ResultSet set = ps.executeQuery();
            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(func.apply(set));
            return list;
        }
    }

    public <T> Optional<T> getOne(String query, ConvertingFunction<T> func, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params)
                ps.setObject(i++, param);

            ResultSet set = ps.executeQuery();
            if (set.next())
                return Optional.ofNullable(func.apply(set));
            else return Optional.empty();
        }
    }

    private PreparedStatement primaryKeyStatement(String query, Collection<Object> keys) throws SQLException {
        List<? extends TableColumn> pk_columns = schema.getPrimaryKeys();
        if (pk_columns == null)
            throw new IllegalStateException("Table " + schema.getName() + " has no primary key(s), therefore requesting an entity by the primary key(s) is impossible");

        if (keys.size() != pk_columns.size())
            throw new IllegalArgumentException("Number of provided primary keys does not match number of primary keys in the table");

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

                TableColumn column = pk_columns.get(param);

                if (SqlType.from(value.getClass()) != column.getSqlType()) {
                    throw new IllegalArgumentException(
                            "Primary key " + param + " value " + value + " is incompatible with column "
                                    + column.toColumnSql()
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

    public <T> Optional<T> getByPrimaryKey(ConvertingFunction<T> func, Object... keys) {
        return getByPrimaryKey(func, Arrays.asList(keys));
    }

    public <T> Optional<T> getByPrimaryKey(ConvertingFunction<T> func, Collection<Object> keys) throws IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(get_by_primary_key_query, keys)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(func.apply(rs));
            else return Optional.empty();
        }  catch (SQLException e) {
            throw PostgresErrorHandler.handle(get_by_primary_key_query, e);
        }
    }

    public boolean existsByPrimaryKey(Object... keys) throws IllegalStateException, IllegalArgumentException {
        return existsByPrimaryKey(Arrays.asList(keys));
    }
    public boolean existsByPrimaryKey(Collection<Object> keys) throws IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(exists_by_primary_key_query, keys)) {
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getBoolean(1);
        }  catch (SQLException e) {
            throw PostgresErrorHandler.handle(exists_by_primary_key_query, e);
        }
    }
    public boolean existsWhere(String where, Object... values) throws IllegalStateException, IllegalArgumentException {
        return existsWhere(where, Arrays.asList(values));
    }
    public boolean existsWhere(String where, Collection<Object> values) throws IllegalStateException, IllegalArgumentException {
        String query = formatQuery("SELECT EXISTS (SELECT 1 FROM :name WHERE " + where + ")", this.schema);
        try (PreparedStatement ps = db.prepareStatement(query)) {
            ResultSet rs = setValues(ps, values).executeQuery();
            return rs.next() && rs.getBoolean(1);
        }  catch (SQLException e) {
            throw PostgresErrorHandler.handle(query, e);
        }
    }

    public boolean deleteByPrimaryKey(Object... keys) {
        return deleteByPrimaryKey(Arrays.asList(keys));
    }

    public boolean deleteByPrimaryKey(Collection<Object> keys) throws IllegalStateException, IllegalArgumentException {
        try (PreparedStatement ps = primaryKeyStatement(delete_by_primary_key_query, keys)) {
            return ps.executeUpdate() != 0;
        } catch (SQLException e) {
            throw PostgresErrorHandler.handle(delete_by_primary_key_query, e);
        }
    }

    public int truncate() throws SQLException {
        try (Statement s = db.createStatement()) {
            return s.executeUpdate("TRUNCATE TABLE " + schema.getName());
        }
    }
    public int drop() throws SQLException {
        try (Statement s = db.createStatement()) {
            return s.executeUpdate("DROP TABLE " + schema.getName());
        }
    }

}
