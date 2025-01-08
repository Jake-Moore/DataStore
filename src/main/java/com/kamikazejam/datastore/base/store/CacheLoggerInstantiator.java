package com.kamikazejam.datastore.base.store;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.log.LoggerService;
import org.jetbrains.annotations.NotNull;

public interface CacheLoggerInstantiator {

    @NotNull
    LoggerService instantiate(Cache<?,?> cache);

}
