package com.kamikazejam.datastore.event.profile;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player logs out, just before their caches are going to be unloaded.<br>
 */
@Getter
@SuppressWarnings("unused")
public class StoreProfilePreLogoutEvent extends Event {

    private final Player player;
    public StoreProfilePreLogoutEvent(Player player) {
        this.player = player;
    }

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() { return handlers; }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
