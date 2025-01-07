package com.kamikazejam.datastore.framework;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.util.*;

@SuppressWarnings({"unchecked", "UnusedReturnValue"})
public abstract class BaseDocument<T extends BaseDocument<T>> {
    private static final Logger log = LoggerFactory.getLogger(BaseDocument.class);
    @JsonIgnore
    public final UUID uuid = UUID.randomUUID();

    @Id
    @JsonProperty("_id")
    public final FieldWrapper<String> id = new FieldWrapper<>("_id", UUID.randomUUID().toString(), String.class);
    @JsonProperty("version")
    public final FieldWrapper<Long> version = new FieldWrapper<>("version", 0L, Long.class);
    @JsonIgnore
    private boolean readOnly;
    @JsonIgnore
    private boolean initialized = false;

    protected BaseDocument() {
        this(true);
    }

    private BaseDocument(boolean readOnly) {
        this.readOnly = readOnly;
    }

    protected final void initialize() {
        if (initialized) { return; }

        // Set parent reference for all fields (including id and version)
        getAllFields().forEach(field -> field.setParent(this));

        initialized = true;
    }

    public boolean isInitialized() {
        for (FieldWrapper<?> field : getAllFields()) {
            if (!field.hasParent()) {
                System.out.println("!!! Field " + field.getName() + " has no parent !!!");
                return false;
            }
        }
        return true;
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Document not initialized. Call initialize() after construction.");
        }
    }

    @NotNull
    protected abstract Set<FieldWrapper<?>> getCustomFields();

    @NotNull
    protected Set<FieldWrapper<?>> getAllFields() {
        Set<FieldWrapper<?>> fields = new HashSet<>(getCustomFields());
        fields.add(id);
        fields.add(version);
        return fields;
    }

    @NotNull
    protected Map<String, FieldWrapper<?>> getAllFieldsMap() {
        Map<String, FieldWrapper<?>> map = new HashMap<>();
        for (FieldWrapper<?> field : getAllFields()) {
            map.put(field.getName(), field);
        }
        return map;
    }

    @JsonIgnore
    protected void setReadOnly(boolean readOnly) {
        ensureInitialized();
        this.readOnly = readOnly;
    }
    
    @JsonIgnore
    public boolean isReadOnly() {
        ensureInitialized();
        return readOnly;
    }

    @NotNull
    T setReadOnly() {
        ensureInitialized();
        setReadOnly(true);
        return (T) this;
    }

    @NotNull
    T setModifiable() {
        ensureInitialized();
        setReadOnly(false);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    protected T deepCopy() {
        ensureInitialized();
        try {
            return JacksonUtil.deepCopy((T) this);
        } catch (Exception e) {
            log.error("Failed to deep copy document: {}", e.getMessage());
            throw new RuntimeException("Failed to deep copy document", e);
        }
    }

    /**
     * Copies all field values from another instance into this instance
     * @param source The source instance to copy values from
     */
    @SuppressWarnings("unchecked")
    protected T copyFieldsFrom(T source) {
        ensureInitialized();
        if (isReadOnly()) {
            throw new IllegalStateException("Cannot copy fields into a read-only document");
        }
        Map<String, FieldWrapper<?>> targetFields = getAllFieldsMap();
        Map<String, FieldWrapper<?>> sourceFields = source.getAllFieldsMap();

        for (Map.Entry<String, FieldWrapper<?>> entry : sourceFields.entrySet()) {
            FieldWrapper<?> targetField = targetFields.get(entry.getKey());
            if (targetField == null) {
                throw new IllegalStateException("Field " + entry.getKey() + " not found in target");
            }
            ((FieldWrapper<Object>)targetField).set(((FieldWrapper<Object>)entry.getValue()).get());
        }
        return (T) this;
    }
}