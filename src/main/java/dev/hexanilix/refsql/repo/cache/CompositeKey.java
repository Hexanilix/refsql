package dev.hexanilix.refsql.repo.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

public final class CompositeKey {

    private final Object[] values;
    private final int hashCode;

    public CompositeKey(@NotNull Collection<?> values) {
        this(values.toArray());
    }
    public CompositeKey(Object... values) {
        this.values = values;
        this.hashCode = Arrays.deepHashCode(this.values);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || !(obj instanceof CompositeKey other)
                || Arrays.deepEquals(this.values, other.values);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

}