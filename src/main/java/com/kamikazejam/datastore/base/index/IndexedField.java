package com.kamikazejam.datastore.base.index;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * All IndexFields are assumed to be unique (only have one Store with that value)
 */
@Getter @SuppressWarnings("unused")
public abstract class IndexedField<X extends Store<X, ?>, T> {
    private final @NotNull Cache<?, X> cache;
    private final @NotNull String name;
    public IndexedField(@NotNull Cache<?, X> cache, @NotNull String name) {
        this.cache = cache;
        this.name = name;
    }

    public abstract boolean equals(@Nullable T a, @Nullable T b);

    public abstract <K, Y extends Store<Y, K>> T getValue(Y store);

    public abstract @NotNull String toString(@NotNull Object value);

    public abstract @NotNull T fromString(@NotNull String value);
}
