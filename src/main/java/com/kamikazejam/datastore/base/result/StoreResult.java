package com.kamikazejam.datastore.base.result;

import com.kamikazejam.datastore.base.Cache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A wrapper around CompletableFuture that provides convenient methods for handling
 * both async and sync completions in the Bukkit/Spigot environment.
 * 
 * @param <T> The type of result this StoreResult will complete with
 */
@Getter
@SuppressWarnings("unused")
public class StoreResult<T> {
    private final CompletableFuture<T> future;
    private final Cache<?, ?> cache;

    private StoreResult(CompletableFuture<T> future, Cache<?, ?> cache) {
        this.future = future;
        this.cache = cache;
    }

    /**
     * Creates a new StoreResult from a CompletableFuture and a Cache.
     * The Cache is used to schedule sync tasks on the main thread when needed.
     */
    public static <T> StoreResult<T> of(@NotNull CompletableFuture<T> future, @NotNull Cache<?, ?> cache) {
        return new StoreResult<>(future, cache);
    }

    /**
     * Creates a completed StoreResult with the given value.
     */
    public static <T> StoreResult<T> completedResult(@NotNull T value, @NotNull Cache<?, ?> cache) {
        return new StoreResult<>(CompletableFuture.completedFuture(value), cache);
    }

    /**
     * Creates a failed StoreResult with the given exception.
     */
    public static <T> StoreResult<T> failedResult(@NotNull Throwable ex, @NotNull Cache<?, ?> cache) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return new StoreResult<>(future, cache);
    }

    /**
     * Handle the result asynchronously when it completes.
     * This is similar to CompletableFuture's whenComplete but with a more descriptive name.
     */
    public StoreResult<T> handleAsync(@NotNull BiConsumer<T, Throwable> handler) {
        future.whenComplete(handler);
        return this;
    }

    /**
     * Handle the result on the main thread when it completes.
     * This is particularly useful when you need to do Bukkit API calls that must be sync.
     */
    public StoreResult<T> handleSync(@NotNull BiConsumer<T, Throwable> handler) {
        future.whenComplete((result, throwable) -> 
            cache.runSync(() -> handler.accept(result, throwable))
        );
        return this;
    }

    /**
     * Handle only successful completion asynchronously.
     */
    public StoreResult<T> onSuccessAsync(@NotNull Consumer<T> handler) {
        future.thenAccept(handler);
        return this;
    }

    /**
     * Handle only successful completion on the main thread.
     */
    public StoreResult<T> onSuccessSync(@NotNull Consumer<T> handler) {
        future.thenAccept(result -> 
            cache.runSync(() -> handler.accept(result))
        );
        return this;
    }

    /**
     * Handle only failures asynchronously.
     */
    public StoreResult<T> onFailureAsync(@NotNull Consumer<Throwable> handler) {
        future.exceptionally(throwable -> {
            handler.accept(throwable);
            return null;
        });
        return this;
    }

    /**
     * Handle only failures on the main thread.
     */
    public StoreResult<T> onFailureSync(@NotNull Consumer<Throwable> handler) {
        future.exceptionally(throwable -> {
            cache.runSync(() -> handler.accept(throwable));
            return null;
        });
        return this;
    }

    /**
     * Transform the result into another type asynchronously.
     */
    public <U> StoreResult<U> transformAsync(@NotNull Function<T, U> transformer) {
        return new StoreResult<>(future.thenApply(transformer), cache);
    }

    /**
     * Transform the result into another type on the main thread.
     */
    public <U> StoreResult<U> transformSync(@NotNull Function<T, U> transformer) {
        CompletableFuture<U> transformed = new CompletableFuture<>();
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                transformed.completeExceptionally(throwable);
            } else {
                cache.runSync(() -> {
                    try {
                        transformed.complete(transformer.apply(result));
                    } catch (Throwable t) {
                        transformed.completeExceptionally(t);
                    }
                });
            }
        });
        return new StoreResult<>(transformed, cache);
    }

}