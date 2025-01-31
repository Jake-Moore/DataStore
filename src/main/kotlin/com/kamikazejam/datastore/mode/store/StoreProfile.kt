package com.kamikazejam.datastore.mode.store

import com.google.common.base.Preconditions
import com.kamikazejam.datastore.base.Collection
import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.serialization.SerializationUtil.USERNAME_FIELD
import com.kamikazejam.datastore.base.serialization.serializer.java.UUIDSerializer
import com.kamikazejam.datastore.mode.profile.StoreProfileCollection
import com.kamikazejam.datastore.util.PlayerUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import java.util.Objects
import java.util.UUID

@Suppress("unused")
@Serializable
abstract class StoreProfile<T : StoreProfile<T>>(
    // Pass these up to the StoreObject implementation
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val version: Long,
    @SerialName(USERNAME_FIELD)
    open val username: String?,
) : Store<T, UUID> {

    // ----------------------------------------------------- //
    //                     CRUD Helpers                      //
    // ----------------------------------------------------- //

    override fun delete(): AsyncDeleteHandler {
        return getCollection().delete(this.id)
    }



    // ----------------------------------------------------- //
    //                       API Methods                     //
    // ----------------------------------------------------- //

    @kotlinx.serialization.Transient @Transient
    override var valid: Boolean = true
        protected set

    @kotlinx.serialization.Transient @Transient
    private var collection: StoreProfileCollection<T>? = null

    override fun getCollection(): Collection<UUID, T> {
        return collection ?: throw IllegalStateException("Collection is not set")
    }



    // ----------------------------------------------------- //
    //                   Data Class Methods                  //
    // ----------------------------------------------------- //
    abstract fun copyHelper(username: String?): T



    // ----------------------------------------------------- //
    //                   Internal Methods                    //
    // ----------------------------------------------------- //

    override fun initialize(collection: Collection<UUID, T>) {
        if (this.collection == null) {
            Preconditions.checkNotNull(collection, "Collection cannot be null")
            require(collection is StoreProfileCollection<T>) { "Collection must be a StoreProfileCollection" }
            this.collection = collection
        }
    }

    override fun hashCode(): Int {
        return Objects.hash(this.id)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is StoreProfile<*>) return false
        return this.id == other.id
    }

    override fun invalidate() {
        this.valid = false
    }



    // ----------------------------------------------------- //
    //                    Profile Methods                    //
    // ----------------------------------------------------- //
    @kotlinx.serialization.Transient @Transient
    private var player: Player? = null

    val uniqueId: UUID
        get() = this.id

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

    /**
     * If the [Player] behind this [StoreProfile] is online AND valid
     * @return Iff the [Player] is truly online
     */
    val isOnlineAndValid: Boolean
        get() {
            // Fetch the player and check if they're online
            this.player = Bukkit.getPlayer(this.uniqueId)
            return PlayerUtil.isFullyValidPlayer(this.player)
        }

    /**
     * If the [Player] behind this [StoreProfile] is online
     * @return Iff the [Player] is online (does not check valid state)
     */
    val isOnline: Boolean
        get() {
            // Fetch the player and check if they're online
            this.player = Bukkit.getPlayer(this.uniqueId)
            this.player?.let { player ->
                return player.isOnline
            }
            return false
        }
}
