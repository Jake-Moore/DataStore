package com.kamikazejam.datastore.base.exception;

import com.kamikazejam.datastore.base.Cache;

@SuppressWarnings("unused")
public class CacheException extends Exception {

    public CacheException(Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception");
    }

    public CacheException(String message, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message);
    }

    public CacheException(String message, Throwable cause, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message, cause);
    }

    public CacheException(Throwable cause, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: ", cause);
    }

    public CacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message, cause, enableSuppression, writableStackTrace);
    }
}
