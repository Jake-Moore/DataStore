package com.kamikazejam.datastore.test2.entity.index;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.index.IndexedField;
import com.kamikazejam.datastore.test2.entity.RandomObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BalanceIndex extends IndexedField<RandomObject, Double> {
    public BalanceIndex(@NotNull Cache<?, RandomObject> cache, @NotNull String fieldName) {
        super(cache, fieldName);
    }

    @Override
    public boolean equals(@Nullable Double a, @Nullable Double b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    @Override
    public <K, Y extends Store<K>> Double getValue(Y store) {
        return ((RandomObject) store).getBalance().get();
    }

    @Override
    public @NotNull String toString(@NotNull Object o) {
        return ((Double) o).toString();
    }

    @Override
    public @NotNull Double fromString(@NotNull String s) {
        return Double.parseDouble(s);
    }

}
