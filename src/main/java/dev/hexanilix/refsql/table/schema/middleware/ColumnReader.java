package dev.hexanilix.refsql.table.schema.middleware;

import dev.hexanilix.refsql.ClassSqlRelationDefinition;
import dev.hexanilix.refsql.TypeRelationManager;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

@FunctionalInterface
public interface ColumnReader {
    Object read(ResultSet rs) throws SQLException;

    static @NotNull ColumnReader createFor(
            String name,
            Class<?> type,
            TypeRelationManager typeRelationManager
    ) {
        if (type == String.class) return rs -> rs.getString(name);
        else if (type == long.class) return rs -> rs.getLong(name);
        else if (type == int.class) return rs -> rs.getInt(name);
        else if (type == short.class) return rs -> rs.getShort(name);
        else if (type == byte.class) return rs -> rs.getByte(name);
        else if (type == double.class) return rs -> rs.getDouble(name);
        else if (type == float.class) return rs -> rs.getFloat(name);
        else if (type == boolean.class) return rs -> rs.getBoolean(name);
        else if (type == char.class) return rs -> rs.getString(name).charAt(0);

        else if (type == Instant.class) return rs -> rs.getTimestamp(name).toInstant();

        else if (type == java.sql.Date.class) return rs -> rs.getDate(name);
        else if (type == java.sql.Time.class) return rs -> rs.getTime(name);
        else if (type == java.sql.Timestamp.class) return rs -> rs.getTimestamp(name);
        else if (type == java.sql.Array.class) return rs -> rs.getArray(name);

        else if (typeRelationManager != null) {
            ClassSqlRelationDefinition<?> def = typeRelationManager.get(type);
            if (def != null)
                return rs -> def.fromSql(rs, name);
        }

        if (type.isEnum()) {
            Class<? extends Enum> enumType = type.asSubclass(Enum.class);
            return rs -> Enum.valueOf(enumType, rs.getString(name));
        }

        return rs -> rs.getObject(name, type);
    }

}