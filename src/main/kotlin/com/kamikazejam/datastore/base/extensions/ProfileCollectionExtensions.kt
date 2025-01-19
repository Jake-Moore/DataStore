@file:Suppress("unused")

package com.kamikazejam.datastore.base.extensions

import com.kamikazejam.datastore.base.async.handler.crud.AsyncDeleteHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncReadHandler
import com.kamikazejam.datastore.base.async.handler.crud.AsyncUpdateHandler
import com.kamikazejam.datastore.mode.profile.ProfileCollection
import com.kamikazejam.datastore.mode.profile.StoreProfile
import org.bukkit.entity.Player
import org.jetbrains.annotations.NonBlocking
import java.util.*
import java.util.function.Consumer

/**
 * Read a StoreProfile (by player) from this collection (will fetch from database, will create if necessary)
 * @param player The player owning this store.
 * @param cacheStore If we should cache the Store upon retrieval. (if it was found)
 * @return The StoreProfile object. (READ-ONLY)
 */
@NonBlocking
fun <X : StoreProfile<X>> ProfileCollection<X>.read(player: Player, cacheStore: Boolean = true): AsyncReadHandler<UUID, X> {
    return this.read(player.uniqueId, cacheStore)
}

/**
 * Modifies a Store in a controlled environment where modifications are allowed
 * @throws NoSuchElementException if the Store (by this key) is not found
 * @return The updated Store object. (READ-ONLY)
 */
@NonBlocking
fun <X : StoreProfile<X>> ProfileCollection<X>.update(player: Player, updateFunction: Consumer<X>): AsyncUpdateHandler<UUID, X> {
    return this.update(player.uniqueId, updateFunction)
}

/**
 * Deletes a Store (removes from both cache and database)
 */
@NonBlocking
fun <X : StoreProfile<X>> ProfileCollection<X>.delete(player: Player): AsyncDeleteHandler {
    return this.delete(player.uniqueId)
}