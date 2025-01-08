package com.kamikazejam.datastore.mode.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Id;
import java.util.*;

@Getter
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public abstract class StoreObject implements Store<String> {
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
    protected transient StoreObjectCache<? extends StoreObject> cache;
    @Getter(AccessLevel.NONE) @JsonIgnore
    protected transient boolean validObject = true;
    @Getter(AccessLevel.NONE) @JsonIgnore
    protected transient boolean readOnly;
    @Getter(AccessLevel.NONE) @JsonIgnore
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
    //                        Methods                        //
    // ----------------------------------------------------- //
    @Override
    @ApiStatus.Internal
    public void initialize() {
        if (initialized) { return; }
        // Set parent reference for all fields (including id and version)
        getAllFields().forEach(field -> field.setParent(this));
        initialized = true;
    }

    private void ensureValid() {
        if (!initialized) {
            throw new IllegalStateException("Document not initialized. Call initialize() after construction.");
        }
        this.getAllFieldsMap(); // may throw error
    }

    @Override
    @ApiStatus.Internal
    public void setReadOnly(boolean readOnly) {
        this.ensureValid();
        this.readOnly = readOnly;
    }

    @Override
    @ApiStatus.Internal
    public @NotNull Set<FieldWrapper<?>> getAllFields() {
        this.ensureValid();
        Set<FieldWrapper<?>> fields = new HashSet<>(getCustomFields());
        fields.add(id);
        fields.add(version);
        return fields;
    }

    @Override
    @ApiStatus.Internal
    public @NotNull Map<String, FieldWrapper<?>> getAllFieldsMap() {
        Map<String, FieldWrapper<?>> map = new HashMap<>();
        for (FieldWrapper<?> field : getAllFields()) {
            if (map.containsKey(field.getName())) {
                throw new IllegalStateException("Duplicate field name: " + field.getName());
            }
            map.put(field.getName(), field);
        }
        return map;
    }

    @NotNull
    @Override
    public StoreObjectCache<? extends StoreObject> getCache() {
        return cache;
    }

    @Override
    public void setCache(Cache<String, ?> cache) {
        this.cache = (StoreObjectCache) cache;
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