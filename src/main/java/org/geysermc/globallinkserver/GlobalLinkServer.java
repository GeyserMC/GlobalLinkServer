/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.globallinkserver.config.Config;
import org.geysermc.globallinkserver.config.ConfigReader;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.util.CommandUtils;
import org.geysermc.globallinkserver.util.Utils;

@SuppressWarnings("UnstableApiUsage")
public class GlobalLinkServer extends JavaPlugin implements Listener {
    public static Logger LOGGER;
    public static LinkManager linkManager;
    public static Config config;

    public final static Component LINK_INSTRUCTIONS = Component.text("Run the ").color(NamedTextColor.AQUA)
            .append(Component.text("`/link`", NamedTextColor.GREEN))
            .append(Component.text(" command to link your accounts.", NamedTextColor.AQUA));

    public final static Component UNLINK_INSTRUCTIONS = Component.text("To unlink, use ").color(NamedTextColor.AQUA)
            .append(Component.text("`/unlink`", NamedTextColor.RED))
            .append(Component.text("."));

    @Override
    public void onEnable() {
        LOGGER = getLogger();

        config = ConfigReader.readConfig(this);

        linkManager = new LinkManager(config);
        CommandUtils commandUtils = new CommandUtils(linkManager);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, linkManager::cleanupTempLinks, 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (Utils.isLinked(player)) {
                    player.sendActionBar(UNLINK_INSTRUCTIONS);
                } else {
                    player.sendActionBar(LINK_INSTRUCTIONS);
                }
            });
        }, 10, 15);
        getServer().getPluginManager().registerEvents(this, this);

        LifecycleEventManager<@NonNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("link")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(commandUtils::startLink)
                            .then(Commands.argument("code", ArgumentTypes.integerRange())
                                    .executes(commandUtils::linkWithCode)
                            )
                            .build(),
                    "Use this command to link your Java and Bedrock account.",
                    List.of("linkaccount")
            );
            commands.register(
                    Commands.literal("unlink")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(commandUtils::unlink)
                            .build(),
                    "Use this command to unlink your Java and Bedrock account.",
                    List.of("unlinkaccount")
            );
            commands.register(
                    Commands.literal("linkinfo")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(ctx -> {
                                Utils.sendCurrentLinkInfo(Utils.getPlayer(ctx));
                                return 1;
                            })
                            .build(),
                    "Use this command to show information whether you are currently linked.",
                    List.of("info")
            );
        });

        // Set game rules
        World world = getServer().getWorld("world");
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);

        // Make nighttime
        world.setTime(18000);

        getServer().setDefaultGameMode(GameMode.ADVENTURE);

        LOGGER.info("Started Global Linking Server plugin!");
    }

    @EventHandler
    public void onCommands(PlayerCommandSendEvent event) {
        Collection<String> toRemove = new ArrayList<>();
        for (String command : event.getCommands()) {
            if (command.startsWith("link")) {
                continue;
            }

            if (command.startsWith("unlink")) {
                continue;
            }

            if (command.contains("info")) {
                continue;
            }

            toRemove.add(command);
        }

        event.getCommands().removeAll(toRemove);
    }

    @EventHandler
    public void onPlayerLoad(PlayerJoinEvent event) {
        event.joinMessage(null);

        event.getPlayer().setPersistent(false);
        event.getPlayer().setAllowFlight(true);

        // Hide all players from each other
        Bukkit.getOnlinePlayers().forEach(player -> {
            event.getPlayer().hidePlayer(this, player);
            player.hidePlayer(this, event.getPlayer());
        });

        Utils.processJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.quitMessage(null);
        Utils.processLeave(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID && event.getEntity() instanceof Player player) {
            event.setCancelled(true);

            Utils.fakeRespawn(player);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getFoodLevel() < event.getEntity().getFoodLevel()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        event.getListedPlayers().clear();
        event.setNumPlayers(0);
        event.setMaxPlayers(1);
    }

    @EventHandler
    public void onPlayerRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        event.setCancelled(true);
    }
}
