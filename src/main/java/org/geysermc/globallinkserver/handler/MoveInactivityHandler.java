/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.handler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.geysermc.globallinkserver.util.MultiConditionSet;

public final class MoveInactivityHandler implements Listener {
    private static final long TIME_TILL_IDLE_MILLIS = 15 * 60 * 1000; // 15 minutes

    private final Object2LongMap<UUID> lastMoveAction = Object2LongMaps.synchronize(new Object2LongOpenHashMap<>());

    public MoveInactivityHandler(MultiConditionSet<UUID> playerIdleTracker) {
        playerIdleTracker
                .addRemovalCondition(key -> {
                    long lastMovement = lastMoveAction.getLong(key);
                    // if not present, the value will be 0. It should never happen, so if that happens we remove them.
                    return System.currentTimeMillis() - lastMovement >= TIME_TILL_IDLE_MILLIS;
                })
                .addRemovalListener(lastMoveAction::removeLong);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // just to make sure that there aren't any weird edge cases of a player not firing a PlayerMoveEvent immediately
        lastMoveAction.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        int diffX = event.getFrom().getBlockX() - event.getTo().getBlockX();
        int diffY = event.getFrom().getBlockZ() - event.getTo().getBlockZ();
        if (Math.abs(diffX) > 0 || Math.abs(diffY) > 0) {
            lastMoveAction.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }
    }
}
