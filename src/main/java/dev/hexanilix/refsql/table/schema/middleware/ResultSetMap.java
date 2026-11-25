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

    private final HashMap<String, ResultSetColumnMetadata> map = new HashMap<>();
    private final Set<String> duplicates = new HashSet<>();

    public ResultSetMap(@NotNull ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();

        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            var meta = ResultSetColumnMetadata.of(metadata, i);
            String name = metadata.getColumnLabel(i);

            if (duplicates.contains(name))
                map.put(name + ":" + meta.table() , meta);

            if (map.containsKey(name)) {
                var old = map.remove(name);
                map.put(name + ":" + old.table(), old);
                duplicates.add(name);

                map.put(name + ":" + meta.table() , meta);
            } else
                map.put(name, meta);
        }
    }

    public @Nullable ResultSetColumnMetadata get(
            @NotNull String name, String table) {
        var r = map.get(name);
        return r != null ? r : map.get(name + ":" + table);
    }

    public HashMap<String, ResultSetColumnMetadata> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}

