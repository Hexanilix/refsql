package dev.hexanilix.refsql.repo;

import java.util.Collection;
import java.util.Optional;

public interface TablePrimaryKeyRequester<T> {

    Optional<T> getByPrimaryKey(Collection<?> keys);
    Optional<T> getByPrimaryKey(Object... keys);

    boolean existsByPrimaryKey(Collection<?> keys);
    boolean existsByPrimaryKey(Object... keys);

}
