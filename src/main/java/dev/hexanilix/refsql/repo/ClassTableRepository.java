package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.queries.select.SelectModifiers;
import dev.hexanilix.refsql.table.FieldTableColumn;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ClassTableRepository<T> extends BaseTableRepository<ClassTableSchema<T>> {

    // You can probably still have custom mapping for these. I'll add this if needed
//    private static final List<Class<?>> RESTRICTED_TYPES = List.of(
//            // All SQL types cannot have a custom mapping
//            java.sql.Timestamp.class,
//            java.sql.Time.class
//            Instant.class,
//    );

    // Queries
    public ClassTableRepository(@NotNull ClassTableSchema<T> schema, Connection db) {
        super(schema, db);
    }

    public List<T> getAll() {
        return super.getAll(schema.getConvertingFunction());
    }

    public List<T> getAll(@NotNull SelectModifiers modifs) throws SQLException {
        return super.getAll(modifs, schema.getConvertingFunction());
    }

    public List<T> getList(String query, Object @NotNull ... params) throws SQLException {
        return getList(
                query,
                schema.getConvertingFunction(),
                params
        );
    }

    public Optional<T> getOne(String query, Object @NotNull ... params) throws SQLException {
        return getOne(
                query,
                schema.getConvertingFunction(),
                params
        );
    }

    public int update(String query, Object @NotNull ... params) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(formatQuery(query, this.getSchema()))) {
            int i = 1;
            for (Object param : params)
                ps.setObject(i++, param);

            return ps.executeUpdate();
        }
    }

    private Object getSqlTypeFromField(@NotNull Field field, Object entity) throws SQLException {
        Object sql_value;
        try {
            field.setAccessible(true);
            Object value = field.get(entity);
            sql_value = SqlType.convert(value, schema.getTypeRelationManager());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed conversion to SQL type for "
                            + entity.getClass() + ":" + field.getName()
            );
        }
        return sql_value;
    }

    public boolean upsert(T entity) {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            int i = 1;
            for (FieldTableColumn c : schema.getColumns())
                s.setObject(i++, getSqlTypeFromField(c.getField(), entity));

            return s.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Performs an upsert batch
     * @param entities
     * @return whether all entities were upsert
     */
    public boolean upsert(@NotNull Collection<T> entities) {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            for (T entity : entities) {
                int i = 1;
                for (FieldTableColumn c : schema.getColumns())
                    s.setObject(i++, getSqlTypeFromField(c.getField(), entity));

                s.addBatch();
            }
            return s.executeBatch().length == entities.size();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<T> getByPrimaryKey(Object... keys) {
        return this.getByPrimaryKey(schema.getConvertingFunction(), keys);
    }

    public Optional<T> getByPrimaryKey(Collection<Object> keys) throws IllegalStateException, IllegalArgumentException {
        return this.getByPrimaryKey(schema.getConvertingFunction(), keys);
    }


}
