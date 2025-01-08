package com.kamikazejam.datastore.mode.profile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.base.field.FieldWrapper;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Id;
import java.util.*;

@Getter
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public abstract class StoreProfile implements Store<UUID> {
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
    protected transient StoreProfileCache<? extends StoreProfile> cache;
    protected transient @Nullable Player player = null;

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
    protected StoreProfile() {
        this(true);
    }
    private StoreProfile(boolean readOnly) {
        this.readOnly = readOnly;
    }

    // ----------------------------------------------------- //
    //                     Store Methods                     //
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
    public StoreProfileCache<? extends StoreProfile> getCache() {
        return cache;
    }

    @Override
    public void setCache(Cache<UUID, ?> cache) {
        this.cache = (StoreProfileCache) cache;
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
            Optional<String> idName = IdUtilLocal.getName(this.getUniqueId());
            idName.ifPresent(name ->
                    this.cache.update(this.getId(), profile -> profile.username.set(name))
            );
            return idName;
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
