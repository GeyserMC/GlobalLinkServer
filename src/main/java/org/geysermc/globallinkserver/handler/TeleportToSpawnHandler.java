/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.handler;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class TeleportToSpawnHandler implements Listener {
    private final Location spawn;

    public TeleportToSpawnHandler(Location spawn) {
        this.spawn = spawn;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        player.setRespawnLocation(spawn);
        teleport(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(true);

            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                teleport(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setCancelled(true);
        teleport(event.getEntity());
    }

    private void teleport(Player player) {
        player.teleport(spawn);
        player.setFallDistance(0);
        //noinspection DataFlowIssue we know it can't be null
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
}
