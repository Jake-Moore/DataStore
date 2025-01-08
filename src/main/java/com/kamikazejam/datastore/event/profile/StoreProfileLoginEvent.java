package com.kamikazejam.datastore.event.profile;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player logs in, after their caches have been loaded
 */
@Getter
@SuppressWarnings("unused")
public class StoreProfileLoginEvent extends Event {

    private final Player player;
    public StoreProfileLoginEvent(Player player) {
        this.player = player;
    }

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() {
        return handlers;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

}
