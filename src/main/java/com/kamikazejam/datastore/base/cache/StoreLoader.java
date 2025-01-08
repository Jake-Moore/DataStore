package com.kamikazejam.datastore.base.cache;

import com.kamikazejam.datastore.base.Store;

import java.util.Optional;

/**
 * This class is responsible for loading a {@link Store} when requested from a Cache.
 */
@SuppressWarnings("rawtypes")
public interface StoreLoader<X extends Store> {

    Optional<X> fetch(boolean saveToLocalCache);

}
