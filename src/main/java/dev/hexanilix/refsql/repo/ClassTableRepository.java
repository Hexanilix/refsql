package dev.hexanilix.refsql.repo;

import dev.hexanilix.refsql.SqlType;
import dev.hexanilix.refsql.repo.cache.TableSnapshot;
import dev.hexanilix.refsql.table.schema.ClassTableSchema;
import dev.hexanilix.refsql.table.schema.column.FieldTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ClassTableRepository<T> extends BaseTableRepository<ClassTableSchema<T>> implements TablePrimaryKeyRequester<T> {

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

    public List<T> getList(String query, Object @NotNull ... params) throws SQLException {
        return super.getList(
                query,
                schema.getConvertingFunction(),
                params
        );
    }

    public Optional<T> getOne(String query, Object @NotNull ... params) throws SQLException {
        return super.getOne(
                query,
                schema.getConvertingFunction(),
                params
        );
    }

    public List<T> page(String where, String order_by, long offset, long limit, @Nullable Collection<?> values) {
        return super.page(where, order_by, offset, limit, schema.getConvertingFunction(), values);
    }
    public List<T> page(String where, String order_by, long offset, long limit, Object... values) {
        return super.page(where, order_by, offset, limit, schema.getConvertingFunction(), Arrays.asList(values));
    }

    private Object getSqlTypeFromField(@NotNull Field field, @NotNull Object entity) throws SQLException {
        Object sql_value;
        try {
            field.setAccessible(true);
            Object value = field.get(entity);
            sql_value = SqlType.toSqlValue(value, schema.getTypeRelationManager());
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed conversion to SQL type for "
                            + entity.getClass() + ":" + field.getName()
            );
        }
        return sql_value;
    }

    /**
     *
     * @param entity
     * @return Whether a row changed or was created
     */
    public boolean upsert(@NotNull T entity) {
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
     * @return Array with the amount of created/wffected rows per entity upsert
     * (0 - nothing changed, 1 - row was updated/created)
     */
    public int[] upsert(@NotNull Collection<@NotNull T> entities) {
        try (PreparedStatement s = db.prepareStatement(upsert_query)) {
            for (T entity : entities) {
                int i = 1;
                for (FieldTableColumn c : schema.getColumns())
                    s.setObject(i++, getSqlTypeFromField(c.getField(), entity));

                s.addBatch();
            }
            return s.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<T> getByPrimaryKey(Object... keys) {
        return this.getByPrimaryKey(schema.getConvertingFunction(), keys);
    }

    @Override
    public Optional<T> getByPrimaryKey(Collection<?> keys) throws IllegalStateException, IllegalArgumentException {
        return this.getByPrimaryKey(schema.getConvertingFunction(), keys);
    }


    public TableSnapshot<T> createPrimaryKeySnapshot() {
        return super.createPrimaryKeySnapshot(schema.getConvertingFunction());
    }
}
