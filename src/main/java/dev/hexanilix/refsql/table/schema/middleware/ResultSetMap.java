package dev.hexanilix.refsql.table.schema.middleware;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ResultSetMap {

    private final HashMap<String, ResultSetColumnMetadata> map;

    public ResultSetMap(@NotNull ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int columnCount = metadata.getColumnCount();

        this.map = HashMap.newHashMap(columnCount * 2);
        Set<String> duplicates = null;

        for (int i = 1; i <= columnCount; i++) {
            String name = metadata.getColumnLabel(i);
            var meta = new ResultSetColumnMetadata(metadata, i, name);

            if (duplicates != null && duplicates.contains(name)) {
                map.put(name + ":" + meta.table(), meta);
            } else {
                var old = map.put(name, meta);
                if (old != null) {
                    map.remove(name);
                    if (duplicates == null) {
                        duplicates = new HashSet<>();
                    }
                    duplicates.add(name);
                    map.put(name + ":" + old.table(), old);
                    map.put(name + ":" + meta.table(), meta);
                }
            }
        }
    }

    public @Nullable ResultSetColumnMetadata get(
            @NotNull String name, String table) {
        var r = map.get(name);
        return r != null ? r : map.get(name + ":" + table);
    }

    public @Nullable ResultSetColumnMetadata getQualified(
            @NotNull String name, @NotNull String qualifiedName) {
        var r = map.get(name);
        return r != null ? r : map.get(qualifiedName);
    }

    public HashMap<String, ResultSetColumnMetadata> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
