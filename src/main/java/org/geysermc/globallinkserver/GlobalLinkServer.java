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
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
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
    public static List<String> permittedCommands;
    public static Plugin plugin;

    public final static Component LINK_INSTRUCTIONS = Component.text("You are not linked. To link, run the ").color(NamedTextColor.AQUA)
            .append(Component.text("`/link`", NamedTextColor.GREEN))
            .append(Component.text(" command.", NamedTextColor.AQUA));

    public final static Component UNLINK_INSTRUCTIONS = Component.text("You are currently linked. To unlink, use ").color(NamedTextColor.AQUA)
            .append(Component.text("`/unlink`", NamedTextColor.RED))
            .append(Component.text("."));

    @Override
    public void onEnable() {
        LOGGER = getLogger();

        plugin = this;

        config = ConfigReader.readConfig(this);

        linkManager = new LinkManager(config);
        CommandUtils commandUtils = new CommandUtils(linkManager);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, linkManager::cleanupTempLinks, 0, 1);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                if (Utils.shouldShowSuggestion(player)) {
                    if (Utils.isLinked(player)) {
                        player.sendActionBar(UNLINK_INSTRUCTIONS);
                    } else {
                        player.sendActionBar(LINK_INSTRUCTIONS);
                    }
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
                            .then(Commands.argument("code", IntegerArgumentType.integer(1, 9999))
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

        // Other changes
        getServer().motd(Component.text("GeyserMC ").color(NamedTextColor.GREEN)
                .append(Component.text("Link ").color(NamedTextColor.AQUA))
                .append(Component.text("Server").color(NamedTextColor.WHITE)));

        getServer().clearRecipes();
        getServer().setDefaultGameMode(GameMode.ADVENTURE);

        LOGGER.info("Started Global Linking plugin!");
    }

    @EventHandler
    public void onCommands(PlayerCommandSendEvent event) {
        if (event.getPlayer().isOp()) {
            return;
        }

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

            if (command.contains("help")) {
                continue;
            }

            toRemove.add(command);
        }

        event.getCommands().removeAll(toRemove);
        permittedCommands = event.getCommands().stream().toList();
    }

    @EventHandler
    public void preCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        String command = event.getMessage();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        if (command.equalsIgnoreCase("help")) {
            event.setCancelled(true);

            if (!Utils.shouldShowSuggestion(player)) {
                player.sendMessage(Component.text("Your linking information is currently unavailable. Please wait!")
                        .color(NamedTextColor.RED));
                return;
            }

            if (Utils.isLinked(player)) {
                player.sendMessage(UNLINK_INSTRUCTIONS);
            } else {
                player.sendMessage(LINK_INSTRUCTIONS);
            }
            return;
        }

        if (!permittedCommands.contains(command)) {
            event.setCancelled(true);
        }
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Prevent crop trampling
        if (event.getAction() == Action.PHYSICAL) {
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
}
