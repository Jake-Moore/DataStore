package com.kamikazejam.datastore.test2.entity.index;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.test2.entity.RandomObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NameIndex extends IndexedField<RandomObject, String> {
    public NameIndex(@NotNull Cache<?, RandomObject> cache, @NotNull String fieldName) {
        super(cache, fieldName);
    }

    @Override
    public boolean equals(@Nullable String a, @Nullable String b) {
        return Objects.equals(a, b);
    }

    @Override
    public <K, Y extends Store<K>> String getValue(Y store) {
        return ((RandomObject) store).getName().get();
    }

    @Override
    public @NotNull String toString(@NotNull Object o) {
        return (String) o;
    }

    @Override
    public @NotNull String fromString(@NotNull String s) {
        return s;
    }
}
