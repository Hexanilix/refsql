package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.queries.Upsert;
import dev.hexanilix.refsql.queries.select.SelectModifiers;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static dev.hexanilix.refsql.StaticJDBCMappings.internalMappingToSql;

public class ClassTableRepository<T> {

    // You can probably still have custom mapping for these. I'll add this if needed
//    private static final List<Class<?>> RESTRICTED_TYPES = List.of(
//            // All SQL types cannot have a custom mapping
//            java.sql.Timestamp.class,
//            java.sql.Time.class
//            Instant.class,
//    );

    public static @NotNull String formatQuery(@NotNull String sql, @NotNull BaseTableSchema<?> schema) {
        return sql.replace(":name", schema.getName());
    }

    protected final Connection db;
    protected final ClassTableSchema<T> schema;

    // Queries
    private final String create_table_if_not_exists_query;
    private final String count_query;
    private final String select_all_query;
    private final String upsert_query;
    private final String get_by_primary_key_query;
    public ClassTableRepository(@NotNull ClassTableSchema<T> schema, Connection db) {
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
            StringBuilder sb = new StringBuilder("SELECT * FROM " + schema.getName() + " WHERE ");
            boolean next = false;
            for (FieldTableColumn key : schema.getPrimaryKeys()) {
                if (next) sb.append(" AND ");
                sb.append(key.getName()).append(" = ?");
                next = true;
            }
            this.get_by_primary_key_query = sb.toString();
        } else this.get_by_primary_key_query = null;
    }

    public ClassTableSchema<T> getSchema() {
        return schema;
    }

    public void ensureTableExists() {
        try (Statement s = db.createStatement()) {
            s.executeUpdate(create_table_if_not_exists_query);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public long count() {
        try (Statement statement = db.createStatement();
             ResultSet rs = statement.executeQuery(count_query)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<T> getAll() throws SQLException {
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(select_all_query)) {

            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(schema.getConvertingFunction().apply(set));
            return list;
        }
    }

    public List<T> getAll(@NotNull SelectModifiers modifs) throws SQLException {
        try (Statement s = db.createStatement();
             ResultSet set = s.executeQuery(
                     select_all_query + " " + modifs.build(schema.getTypeRelationManager())
             )) {

            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(schema.getConvertingFunction().apply(set));
            return list;
        }
    }

    public List<T> getList(String query, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params) {
                ps.setObject(i++, param);
            }

            ResultSet set = ps.executeQuery();

            var c = schema.getConvertingFunction();
            List<T> list = new ArrayList<>();
            while (set.next())
                list.add(c.apply(set));
            return list;
        }
    }

    public Optional<T> getOne(String query, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params)
                ps.setObject(i++, param);

            ResultSet set = ps.executeQuery();
            if (set.next())
                return Optional.ofNullable(schema.getConvertingFunction().apply(set));
            else return Optional.empty();
        }
    }

    /**
     * Converts a class fields value into a compatible SQL type
     *
     * @param field The class field from which to pull the value for SQL use
     * @param obj The class instance
     * @return A JDBC Potsrges compatible object value
     * @param <Obj> Helper type
     * @throws IllegalAccessException Occurs when trying to access an unaccessible field
     * @throws IllegalStateException Occurs when the resulting object is not a SQL compatible value
     */
    private <Obj> Object retrieveObject(@NotNull Field field, Object obj) throws IllegalAccessException, IllegalStateException {
        field.trySetAccessible();
        Object value = field.get(obj);
        if (value != null) {
            Class<Obj> value_clazz = (Class<Obj>) value.getClass();
            boolean to_convert = true;
            if (schema.hasTypeRelationManager()) {
                ClassSqlRelationDefinition<Obj> def =
                        schema.getTypeRelationManager().get(value_clazz);
                if (def != null) {
                    value = def.convertToSql((Obj) value);
                    to_convert = false;
                }
            }
            if (to_convert) {
                if (value.getClass().isEnum()) {
                    value = ((Enum<?>) value).name();
                    to_convert = false;
                }
            }

            // This mapping is always run since it's reasonable to assume that, for example, an Instant or an Enum
            // returned from a conversion is a valid SQL value, but JDBC won't accept it normally, thus the extra conversion
            value = internalMappingToSql(value);
        }

        if (value != null && SqlType.from(value.getClass()) == null)
            throw new IllegalStateException(
                    "Value " + value + " is not a valid Sql type. Got a " + value.getClass() + " after conversion from "
                            + obj.getClass() + ":" + field.getName()
            );

        return value;
    }

    public int update(String query, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params)
                ps.setObject(i++, param);

            return ps.executeUpdate();
        }
    }

    public boolean upsert(T entity) throws SQLException {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            int i = 1;
            for (FieldTableColumn c : schema.getColumns())
                s.setObject(i++, retrieveObject(c.getField(), entity));

            return s.executeUpdate() == 1;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            System.err.println("Error while executing: " + upsert_query);
            throw e;
        }
    }

    public boolean upsert(@NotNull Collection<T> entities) throws SQLException {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            for (T t : entities) {
                int i = 1;
                for (FieldTableColumn c : schema.getColumns())
                    s.setObject(i++, retrieveObject(c.getField(), t));
                s.addBatch();
            }
            return s.executeBatch().length == entities.size();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<T> getByPrimaryKey(Object... keys) throws SQLException {
        List<FieldTableColumn> pk_collumns = schema.getPrimaryKeys();
        if (pk_collumns == null)
            throw new IllegalStateException("Schema has no primary key(s), therefore requesting an entity by the primary key(s) is impossible");

        if (keys.length != pk_collumns.size())
            throw new IllegalArgumentException("Number of provided primary keys does not match number of primary keys in the schema");

        int i = 0;
        try (PreparedStatement s = db.prepareStatement(get_by_primary_key_query)) {
            for (Object key : keys) {
                if (key == null)
                    throw new IllegalStateException("Primary key is null");

                Object value = schema.hasTypeRelationManager() ? schema.getTypeRelationManager().convert(key) : key;

                FieldTableColumn column = pk_collumns.get(i);

                if (value == null || SqlType.from(value.getClass()) != column.getSqlType()) {
                    throw new IllegalArgumentException(
                            "Primary key value is incompatible with column "
                                    + column.toColumnSql());
                }
                s.setObject(++i, value);
            }

            ResultSet set = s.executeQuery();
            if (set.next()) return Optional.of(schema.getConvertingFunction().apply(set));
            else return Optional.empty();
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
