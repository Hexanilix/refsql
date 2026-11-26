package dev.hexanilix.refsql.table.schema.middleware.base;

import dev.hexanilix.refsql.ConvertingFunction;
import dev.hexanilix.refsql.exceptions.MissingColumnInResultSetException;
import dev.hexanilix.refsql.table.schema.BaseTableSchema;
import dev.hexanilix.refsql.table.schema.column.TableColumn;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ColumnMapping;
import dev.hexanilix.refsql.table.schema.middleware.mapping.QueryResult;
import dev.hexanilix.refsql.table.schema.middleware.mapping.ResultSetColumnMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static dev.hexanilix.refsql.table.schema.middleware.DefaultObjectBuilder.getDefaultValueOnNotPresent;

public abstract class ImplementedConvertingFunction<Schema extends BaseTableSchema<?>, Base, T> implements ConvertingFunction<T> {


    protected final Schema schema;
    protected Constructor<T> constructor;
    protected ColumnMapping<TableColumn>[] mappings;

    protected ImplementedConvertingFunction(Schema schema, Constructor<T> constructor, ColumnMapping<TableColumn>[] mappings) {
        this.schema = schema;
        this.constructor = constructor;
        this.mappings = mappings;
    }
    protected ImplementedConvertingFunction(Schema schema, Constructor<T> constructor) {
        this(schema, constructor, null);
    }
    protected ImplementedConvertingFunction(Schema schema) {
        this(schema, null, null);
    }


    protected abstract Base createBase() throws Exception;

    protected abstract void applyToBase(Base base, int index, TableColumn column, ResultSetColumnMetadata meta, Object value) throws Exception;
    protected abstract T buildObject(Base base) throws Exception;

    @Override
    public @Nullable ResultSetColumnMetadata[] getComputedMappings(@NotNull QueryResult qr) throws SQLException {
        return qr.getOrCompute(mappings);
    }

    @Override
    public List<T> applyToList(@NotNull QueryResult result) throws SQLException {
        List<T> objects = new ArrayList<>();
        if (!result.next()) return objects;
        ResultSetColumnMetadata[] precompute = this.getComputedMappings(result);
        do {
            objects.add(apply(result, precompute));
        } while (result.next());
        return objects;
    }

    @Override
    public T apply(QueryResult result) throws SQLException { return apply(result, null); }

    @Override
    public T apply(QueryResult qr, @Nullable ResultSetColumnMetadata[] precompute) throws MissingColumnInResultSetException, SQLException {
        try {
            Base base = createBase();

            for (int i = 0; i < mappings.length; i++) {
                ColumnMapping<TableColumn> mapping = mappings[i];
                TableColumn column = mapping.column();

                ResultSetColumnMetadata meta = precompute != null
                        ? precompute[i]
                        : qr.getColumnMeta(mapping);

                applyToBase(
                        base,
                        i,
                        column,
                        meta,
                        meta != null
                                ? mapping.reader().read(qr, meta)
                                : getDefaultValueOnNotPresent(column, mapping.clazz(), schema)
                );
            }

            return buildObject(base);
        } catch (SQLException | RuntimeException e) {
            throw e;
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "Constructor " + constructor + " threw an exception.",
                    e.getCause()
            );
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Could not instantiate using constructor " + constructor,
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
