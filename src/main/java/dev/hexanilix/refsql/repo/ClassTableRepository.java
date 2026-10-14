package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.queries.Upsert;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ClassTableRepository<T> {

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
        this.upsert_query = Upsert.buildUpsertQuery(schema);

        this.get_by_primary_key_query = schema.hasPrimaryKey()
                ? "SELECT * FROM " + schema.getName() + " WHERE " + schema.getPrimaryKey().getName() + "=?"
                : null;
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

    private <Obj> Object retrieveObject(@NotNull Field field, Object obj) throws IllegalAccessException {
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
        }
        return value;
    }

    public boolean upsert(T entity) throws SQLException {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            int i = 1;
            for (FieldTableColumn c : schema.getColumns()) {
                s.setObject(i++, retrieveObject(c.getField(), entity));
            }
            return s.executeUpdate() == 1;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean upsertMany(@NotNull Collection<T> entities) throws SQLException {
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

    public Optional<T> getByPrimaryKey(Object... primary_keys) throws SQLException {
        // TODO: Add support for multiple primary keys
        if (schema.getPrimaryKey() == null)
            throw new IllegalStateException("Schema has no primary key(s), therefore requesting an entity by the primary key(s) is impossible");

        int i = 1;
        try (PreparedStatement s = db.prepareStatement(get_by_primary_key_query)) {
            for (Object primary_key : primary_keys)
                s.setObject(i++, primary_key);

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
