package com.kamikazejam.datastore.mode.profile

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Cache
import com.kamikazejam.datastore.base.Store
import com.kamikazejam.datastore.base.field.FieldProvider
import com.kamikazejam.datastore.base.field.FieldWrapper
import com.kamikazejam.datastore.util.PlayerUtil
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.*
import java.util.function.Consumer
import javax.persistence.Id

@Suppress("unused")
abstract class StoreProfile<T : StoreProfile<T>> private constructor(
    @field:Transient @field:JsonIgnore override var readOnly: Boolean
) : Store<T, UUID> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (a player uuid)
    @Id
    override val idField: FieldWrapper<UUID> = FieldWrapper.of("_id", null, UUID::class.java)
    override val versionField: FieldWrapper<Long> = FieldWrapper.of("version", 0L, Long::class.java)
    val usernameField: FieldWrapper<String> = FieldWrapper.of("username", null, String::class.java)

    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    @JsonIgnore
    @Transient
    private var cache: StoreProfileCache<T>? = null

    @Transient
    private var player: Player? = null

    @JsonIgnore
    @Transient
    override var valid: Boolean = true
        protected set

    @JsonIgnore
    @Transient
    protected var initialized: Boolean = false


    // ----------------------------------------------------- //
    //                     Constructors                      //
    // ----------------------------------------------------- //
    // For Jackson
    protected constructor() : this(true)

    // ----------------------------------------------------- //
    //                  CRUD Helpers (sync)                  //
    // ----------------------------------------------------- //
    override fun updateSync(updateFunction: Consumer<T>): T {
        return getCache().updateSync(this.id, updateFunction)
    }

    override fun deleteSync() {
        getCache().deleteSync(this.id)
    }

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    @Internal
    override fun initialize() {
        if (initialized) {
            return
        }
        initialized = true // Must set before calling getAllFields because it will want it to be true
        // Set parent reference for all fields (including id and version)
        allFields.forEach { provider: FieldProvider ->
            provider.fieldWrapper.setParent(this)
        }
    }

    private fun ensureValid() {
        check(initialized) { "Document not initialized. Call initialize() after construction." }
        this.validateDuplicateFields() // may throw error
    }

    override fun getCache(): Cache<UUID, T> {
        return cache ?: throw IllegalStateException("Cache is not set")
    }

    @get:Internal
    override val allFields: Set<FieldProvider>
        get() {
            this.ensureValid()
            val fields: MutableSet<FieldProvider> =
                HashSet(getCustomFields())
            fields.add(idField)
            fields.add(versionField)
            fields.add(usernameField)
            return fields
        }

    private fun validateDuplicateFields() {
        val names: MutableSet<String> = HashSet()
        names.add(idField.name)
        names.add(versionField.name)
        names.add(usernameField.name)
        for (provider in getCustomFields()) {
            check(names.add(provider.fieldWrapper.name)) { "Duplicate field name: " + provider.fieldWrapper.name }
        }
    }

    @Suppress("DuplicatedCode")
    @get:Internal
    override val allFieldsMap: Map<String, FieldProvider>
        get() {
            val map: MutableMap<String, FieldProvider> =
                HashMap()
            for (provider in allFields) {
                check(
                    !map.containsKey(provider.fieldWrapper.name)
                ) { "Duplicate field name: " + provider.fieldWrapper.name }
                map[provider.fieldWrapper.name] = provider
            }
            return map
        }

    override fun setCache(cache: Cache<UUID, T>) {
        Preconditions.checkNotNull(cache, "Cache cannot be null")
        require(cache is StoreProfileCache<T>) { "Cache must be a StoreProfileCache" }
        this.cache = cache
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id, this.uniqueId)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is StoreProfile<*>) {
            return false
        }
        return this.idField == other.idField
    }

    override fun invalidate() {
        this.valid = false
    }


    // ----------------------------------------------------- //
    //                    Profile Methods                    //
    // ----------------------------------------------------- //
    /**
     * Get the Player represented by this player
     * @return The Optional - may not be online here
     */
    fun getPlayer(): Player? {
        if (PlayerUtil.isFullyValidPlayer(this.player)) {
            return this.player
        }

        this.player = Bukkit.getPlayer(this.uniqueId)
        if (!PlayerUtil.isFullyValidPlayer(this.player)) {
            this.player = null
        }
        return this.player
    }

    val uniqueId: UUID
        /**
         * Get the UUID of the player
         */
        get() = this.id

    /**
     * Get the Name of the Player
     */
    fun getUsername(): String? {
        if (usernameField.get() == null) {
            // Try to get the name from our IdUtil, and update the object if possible
            val oPlayer: OfflinePlayer? = Bukkit.getOfflinePlayer(this.uniqueId)
            if (oPlayer?.name != null) {
                cache!!.update(this.id) { profile: T -> profile.usernameField.set(oPlayer.name) }
                return oPlayer.name
            }
        }
        return usernameField.get()
    }

    /**
     * Stores the Player object inside this Profile
     */
    @Internal
    fun initializePlayer(player: Player) {
        Preconditions.checkNotNull(player, "Player cannot be null for initializePlayer")
        this.player = player
    }

    /**
     * nullifies the Player object from this Profile
     */
    @Internal
    fun uninitializePlayer() {
        this.player = null
    }

    val isOnlineAndValid: Boolean
        /**
         * Check if the player behind this Profile is online (and valid)
         * @return Iff the Player is online this server
         */
        get() {
            // Fetch the player and check if they're online
            this.player = Bukkit.getPlayer(this.uniqueId)
            return PlayerUtil.isFullyValidPlayer(this.player)
        }

    val isOnline: Boolean
        /**
         * Check if the player behind this Profile is online (and valid)
         * @return Iff the Player is online this server
         */
        get() {
            // Fetch the player and check if they're online
            this.player = Bukkit.getPlayer(this.uniqueId)
            return this.player != null && player!!.isOnline
        }
}