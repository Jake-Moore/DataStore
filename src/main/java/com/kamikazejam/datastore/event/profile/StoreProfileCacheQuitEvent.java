package com.kamikazejam.datastore.event.profile;

import com.kamikazejam.datastore.mode.profile.StoreProfile;
import com.kamikazejam.datastore.mode.profile.StoreProfileCache;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player's StoreProfile is being removed from the cache.
 * Cache data can be accessed in this event, using the cache field.
 */
@Getter
@SuppressWarnings("unused")
public class StoreProfileCacheQuitEvent<X extends StoreProfile<X>> extends Event {

    private final Player player;
    private final StoreProfileCache<X> cache;
    private final X profile;

    public StoreProfileCacheQuitEvent(Player player, StoreProfileCache<X> cache, X profile) {
        this.player = player;
        this.cache = cache;
        this.profile = profile;
    }

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() { return handlers; }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
