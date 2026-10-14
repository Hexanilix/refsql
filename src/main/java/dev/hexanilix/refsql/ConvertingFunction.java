package dev.hexanilix.refsql;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public interface ConvertingFunction<T> {
    T apply(ResultSet resultSet) throws SQLException;

    default List<T> applyToList(@NotNull ResultSet resultSet) throws SQLException {
        ArrayList<T> list = new ArrayList<>();

        while (resultSet.next())
            list.add(apply(resultSet));
        return list;
    }

}
