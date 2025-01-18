/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver.util;

import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public class Utils {
    public static Player contextExecutor(CommandContext<CommandSourceStack> ctx) {
        //noinspection DataFlowIssue we know it can't be null
        return (Player) ctx.getSource().getExecutor();
    }

    public static void fakeRespawn(Player player) {
        player.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
        player.setFallDistance(0);
        //noinspection DataFlowIssue we know it can't be null
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }
}
