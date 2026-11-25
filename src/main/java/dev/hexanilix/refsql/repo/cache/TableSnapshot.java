package dev.hexanilix.refsql.repo.cache;

import dev.hexanilix.refsql.repo.TablePrimaryKeyRequester;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TableSnapshot<T> implements Iterable<Map.Entry<CompositeKey, T>>, TablePrimaryKeyRequester<T> {

    private final HashMap<CompositeKey, T> map;
    public TableSnapshot(HashMap<CompositeKey, T> map) {
        this.map = map;
    }

    public Optional<T> getByPrimaryKey(Object... keys) {
        return Optional.ofNullable(map.get(new CompositeKey(keys)));
    }
    public Optional<T> getByPrimaryKey(@NotNull Collection<?> keys) {
        return getByPrimaryKey(keys.toArray());
    }

    public boolean existsByPrimaryKey(Object... keys) {
        return map.containsKey(new CompositeKey(keys));
    }
    public boolean existsByPrimaryKey(@NotNull Collection<?> keys) {
        return existsByPrimaryKey(keys.toArray());
    }

    public @NotNull Collection<T> values() {
        return map.values();
    }

    public int size() {
        return map.size();
    }

    @Override
    public @NotNull Iterator<Map.Entry<CompositeKey, T>> iterator() {
        return map.entrySet().iterator();
    }

    @Override
    public void forEach(Consumer<? super Map.Entry<CompositeKey, T>> action) {
        map.entrySet().forEach(action);
    }

    public void forEach(BiConsumer<CompositeKey, T> consumer) {
        map.forEach(consumer);
    }

}
