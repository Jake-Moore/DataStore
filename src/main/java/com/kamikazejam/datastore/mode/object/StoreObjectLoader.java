package com.kamikazejam.datastore.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.cache.StoreLoader;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Optional;

@Getter
@Setter
public class StoreObjectLoader<X extends StoreObject> implements StoreLoader<X> {
    private final StoreObjectCache<X> cache;
    private final String identifier;

    private WeakReference<X> store = null;
    private boolean loadedFromLocal = false;

    StoreObjectLoader(@NotNull StoreObjectCache<X> cache, String identifier) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(identifier);
        this.cache = cache;
        this.identifier = identifier;
    }

    @SuppressWarnings("SameParameterValue")
    private void load(boolean fromLocal) {
        if (fromLocal) {
            Optional<X> local = cache.getLocalStore().get(identifier);
            if (local.isPresent()) {
                // Ensure our Store is valid (not recently deleted)
                X store = local.get();
                if (store.isValid()) {
                    this.store = new WeakReference<>(store);
                    loadedFromLocal = true;
                    return;
                }

                // Nullify the reference if the Store is invalid
                // Don't quit, we could in theory still pull from the database
                cache.getLocalStore().remove(identifier);
                this.store = new WeakReference<>(null);
            }
        }

        Optional<X> db = cache.getFromDatabase(identifier, true);
        db.ifPresent(x -> {
            store = new WeakReference<>(x);
            loadedFromLocal = false;
        });
    }

    @Override
    public Optional<X> fetch(boolean saveToLocalCache) {
        load(true);

        if (store == null) {
            return Optional.empty();
        }

        // Double check validity here too
        @Nullable X p = store.get();
        if (p != null && !p.isValid()) {
            store = new WeakReference<>(null);
            return Optional.empty();
        }

        // Save to local cache if necessary
        if (saveToLocalCache && p != null && !loadedFromLocal) {
            this.cache.cache(p);
        }

        // Ensure the Store has its cache set
        if (p != null) {
            p.setCache(cache);
        }
        return Optional.ofNullable(p);
    }
}
