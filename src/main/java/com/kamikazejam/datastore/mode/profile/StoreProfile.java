package com.kamikazejam.datastore.mode.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Preconditions;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.field.FieldProvider;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.datastore.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Id;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings({"rawtypes", "unused"})
public abstract class StoreProfile<T extends StoreProfile<T>> implements Store<T, UUID> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (a player uuid)
    @Id
    public final @NotNull FieldWrapper<UUID> id = FieldWrapper.of("_id", null, UUID.class);
    public final @NotNull FieldWrapper<Long> version = FieldWrapper.of("version", 0L, Long.class);
    public final @NotNull FieldWrapper<String> username = FieldWrapper.of("username", null, String.class);


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    @JsonIgnore
    protected transient StoreProfileCache<T> cache;
    protected transient @Nullable Player player = null;

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
    protected StoreProfile() {
        this(true);
    }
    private StoreProfile(boolean readOnly) {
        this.readOnly = readOnly;
    }

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
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
    //                     Store Methods                     //
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
        fields.add(username);
        return fields;
    }

    private void validateDuplicateFields() {
        Set<String> names = new HashSet<>();
        names.add(id.getName());
        names.add(version.getName());
        names.add(username.getName());
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
    public StoreProfileCache<T> getCache() {
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        return cache;
    }

    @Override
    public void setCache(Cache<UUID, T> cache) {
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        if (!(cache instanceof StoreProfileCache<T> oCache)) {
            throw new IllegalArgumentException("Cache must be a StoreProfileCache");
        }
        this.cache = oCache;
    }

    @Override
    public @NotNull FieldWrapper<Long> getVersionField() {
        return this.version;
    }

    @Override
    public @NotNull FieldWrapper<UUID> getIdField() {
        return this.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getUniqueId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) { return true; }
        if (!(o instanceof StoreProfile other)) { return false; }
        return Objects.equals(this.id, other.id);
    }

    @Override
    public @NotNull UUID getId() {
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



    // ----------------------------------------------------- //
    //                    Profile Methods                    //
    // ----------------------------------------------------- //
    /**
     * Get the Player represented by this player
     * @return The Optional - may not be online here
     */
    @NotNull
    public Optional<Player> getPlayer() {
        if (PlayerUtil.isFullyValidPlayer(this.player)) { return Optional.of(this.player); }

        this.player = Bukkit.getPlayer(this.getUniqueId());
        if (!PlayerUtil.isFullyValidPlayer(this.player)) { this.player = null; }
        return Optional.ofNullable(player);
    }

    /**
     * Get the UUID of the player
     */
    @NotNull
    public UUID getUniqueId() {
        return this.getId();
    }

    /**
     * Get the Name of the Player
     */
    public @NotNull Optional<String> getUsername() {
        if (this.username.get() == null) {
            // Try to get the name from our IdUtil, and update the object if possible
            OfflinePlayer oPlayer = Bukkit.getOfflinePlayer(this.getUniqueId());
            if (oPlayer != null && oPlayer.getName() != null) {
                this.cache.update(this.getId(), profile -> profile.username.set(oPlayer.getName()));
                return Optional.of(oPlayer.getName());
            }
        }
        return Optional.ofNullable(this.username.get());
    }

    /**
     * Stores the Player object inside this Profile
     */
    @ApiStatus.Internal
    public final void initializePlayer(@NotNull Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null for initializePlayer");
        this.player = player;
    }

    /**
     * nullifies the Player object from this Profile
     */
    @ApiStatus.Internal
    public final void uninitializePlayer() {
        this.player = null;
    }

    /**
     * Check if the player behind this Profile is online (and valid)
     * @return Iff the Player is online this server
     */
    public boolean isOnlineAndValid() {
        // Fetch the player and check if they're online
        this.player = Bukkit.getPlayer(this.getUniqueId());
        return PlayerUtil.isFullyValidPlayer(this.player);
    }

    /**
     * Check if the player behind this Profile is online (and valid)
     * @return Iff the Player is online this server
     */
    public boolean isOnline() {
        // Fetch the player and check if they're online
        this.player = Bukkit.getPlayer(this.getUniqueId());
        return this.player != null && this.player.isOnline();
    }
}
