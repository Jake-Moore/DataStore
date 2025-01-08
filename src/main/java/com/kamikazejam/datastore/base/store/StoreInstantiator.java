package com.kamikazejam.datastore.base.store;

import com.kamikazejam.datastore.base.Store;
import org.jetbrains.annotations.NotNull;

public interface StoreInstantiator<K, X extends Store<K>> {

    @NotNull
    X instantiate();

}
