package com.kamikazejam.datastore.mode.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.field.FieldProvider;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Id;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unused"})
public abstract class StoreObject<T extends StoreObject<T>> implements Store<T, String> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (as a user-defined String)
    @Id
    public final @NotNull FieldWrapper<String> id = FieldWrapper.of("_id", UUID.randomUUID().toString(), String.class);
    public final @NotNull FieldWrapper<Long> version = FieldWrapper.of("version", 0L, Long.class);


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    @JsonIgnore
    protected transient StoreObjectCache<T> cache;
    @JsonIgnore
    protected transient boolean validObject = true;
    @JsonIgnore
    protected transient boolean readOnly;
    @JsonIgnore
    protected transient boolean initialized = false;


    // ----------------------------------------------------- //
    //                     Constructors                      //
    // ----------------------------------------------------- //
    // For Jackson
    protected StoreObject() {
        this(true);
    }
    private StoreObject(boolean readOnly) {
        this.readOnly = readOnly;
    }

    // ----------------------------------------------------- //
    //                     CRUD Helpers                      //
    // ----------------------------------------------------- //
    @Override
    public T updateSync(@NotNull Consumer<T> updateFunction) {
        return this.getCache().updateSync(this.getId(), updateFunction);
    }
    @Override
    public void deleteSync() {
        this.getCache().deleteSync(this.getId());
    }

    // ----------------------------------------------------- //
    //                        Methods                        //
    // ----------------------------------------------------- //
    @Override
    @ApiStatus.Internal
    public void initialize() {
        if (initialized) { return; }
        initialized = true; // Must set before calling getAllFields because it will want it to be true
        // Set parent reference for all fields (including id and version)
        getAllFields().forEach(provider -> provider.getFieldWrapper().setParent(this));
    }

    private void ensureValid() {
        if (!initialized) {
            throw new IllegalStateException("Document not initialized. Call initialize() after construction.");
        }
        this.validateDuplicateFields(); // may throw error
    }

    @Override
    @ApiStatus.Internal
    public void setReadOnly(boolean readOnly) {
        this.ensureValid();
        this.readOnly = readOnly;
    }

    @Override
    @ApiStatus.Internal
    public @NotNull Set<FieldProvider> getAllFields() {
        this.ensureValid();
        Set<FieldProvider> fields = new HashSet<>(getCustomFields());
        fields.add(id);
        fields.add(version);
        return fields;
    }

    private void validateDuplicateFields() {
        Set<String> names = new HashSet<>();
        names.add(id.getName());
        names.add(version.getName());
        for (FieldProvider provider : getCustomFields()) {
            if (!names.add(provider.getFieldWrapper().getName())) {
                throw new IllegalStateException("Duplicate field name: " + provider.getFieldWrapper().getName());
            }
        }
    }

    @Override
    @ApiStatus.Internal
    public @NotNull Map<String, FieldProvider> getAllFieldsMap() {
        Map<String, FieldProvider> map = new HashMap<>();
        for (FieldProvider provider : getAllFields()) {
            if (map.containsKey(provider.getFieldWrapper().getName())) {
                throw new IllegalStateException("Duplicate field name: " + provider.getFieldWrapper().getName());
            }
            map.put(provider.getFieldWrapper().getName(), provider);
        }
        return map;
    }

    @NotNull
    @Override
    public StoreObjectCache<T> getCache() {
        return cache;
    }

    @Override
    public void setCache(Cache<String, T> cache) {
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        if (!(cache instanceof StoreObjectCache<T> oCache)) {
            throw new IllegalArgumentException("Cache must be a StoreObjectCache");
        }
        this.cache = oCache;
    }

    @Override
    public @NotNull FieldWrapper<Long> getVersionField() {
        return this.version;
    }

    @Override
    public @NotNull FieldWrapper<String> getIdField() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) { return true; }
        if (!(o instanceof StoreObject other)) { return false; }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public @NotNull String getId() {
        return Objects.requireNonNull(this.id.get(), "Id cannot be null");
    }

    @Override
    public boolean isReadOnly() {
        this.ensureValid();
        return this.readOnly;
    }

    @Override
    public boolean isValid() {
        return this.validObject;
    }

    @Override
    public void invalidate() {
        this.validObject = false;
    }
}
