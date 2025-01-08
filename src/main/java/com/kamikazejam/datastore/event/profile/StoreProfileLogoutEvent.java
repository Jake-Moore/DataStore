package com.kamikazejam.datastore.event.profile;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player logs out, after their caches have been unloaded.<br>
 * Cache data should NOT be accessed in this event, use {@link StoreProfilePreLogoutEvent} instead.
 */
@Getter
@SuppressWarnings("unused")
public class StoreProfileLogoutEvent extends Event {

    private final Player player;

    public StoreProfileLogoutEvent(Player player) {
        this.player = player;
    }

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() { return handlers; }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
