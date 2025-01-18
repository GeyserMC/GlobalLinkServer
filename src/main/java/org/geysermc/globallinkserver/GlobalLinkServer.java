/*
 * Copyright (c) 2021-2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.globallinkserver.config.ConfigReader;
import org.geysermc.globallinkserver.handler.JoinHandler;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.manager.DatabaseManager;
import org.geysermc.globallinkserver.manager.PlayerManager;
import org.geysermc.globallinkserver.service.LinkLookupService;
import org.geysermc.globallinkserver.service.LinkInfoService;
import org.geysermc.globallinkserver.handler.CommandHandler;
import org.geysermc.globallinkserver.util.Utils;

@SuppressWarnings("UnstableApiUsage")
public class GlobalLinkServer extends JavaPlugin implements Listener {
    private static final Set<String> PERMITTED_COMMANDS =
            Set.of("link", "linkaccount", "linkinfo", "info", "unlink", "unlinkaccount", "help");

    private final Cache<UUID, Instant> playerIdleCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .<UUID, Instant>removalListener(notification -> {
                if (notification.wasEvicted() && notification.getCause() == RemovalCause.EXPIRED) {
                    Player player = Bukkit.getPlayer(notification.getKey());
                    if (player != null) {
                        Bukkit.getScheduler().callSyncMethod(this, () -> {
                            player.kick(Components.KICK_IDLE);
                            return null;
                        });
                    }
                }
            })
            .build();

    private LinkLookupService linkLookupService;
    private LinkInfoService linkInfoService;

    @Override
    public void onEnable() {
        var config = ConfigReader.readConfig(this);

        var playerManager = new PlayerManager(FloodgateApi.getInstance());
        var databaseManager = new DatabaseManager(config);
        var linkManager = new LinkManager(playerManager, databaseManager);
        linkLookupService = new LinkLookupService(playerManager, databaseManager);
        linkInfoService = new LinkInfoService(linkLookupService, playerManager);

        var commandUtils = new CommandHandler(linkLookupService, linkInfoService, linkManager, playerManager, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, linkManager::cleanupTempLinks, 0, 1);

        Bukkit.getScheduler()
                .scheduleSyncRepeatingTask(
                        this,
                        () -> {
                            Bukkit.getOnlinePlayers().forEach(player -> {
                                if (linkLookupService.isLookupCompleted(player)) {
                                    if (linkLookupService.isLinkedCached(player)) {
                                        player.sendActionBar(Components.UNLINK_INSTRUCTION);
                                    } else {
                                        player.sendActionBar(Components.LINK_INSTRUCTION);
                                    }
                                }
                            });
                        },
                        10,
                        15);

        var pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new JoinHandler(linkLookupService, playerIdleCache, this), this);

        LifecycleEventManager<@NonNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("link")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(commandUtils::startLink)
                            .then(Commands.argument("code", IntegerArgumentType.integer(0, 9999))
                                    .executes(commandUtils::linkWithCode))
                            .build(),
                    "Use this command to link your Java and Bedrock account.",
                    List.of("linkaccount"));
            commands.register(
                    Commands.literal("unlink")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(commandUtils::unlink)
                            .build(),
                    "Use this command to unlink your Java and Bedrock account.",
                    List.of("unlinkaccount"));
            commands.register(
                    Commands.literal("linkinfo")
                            .requires(ctx -> ctx.getSender() instanceof Player)
                            .executes(ctx -> {
                                linkInfoService.sendCurrentLinkInfo(Utils.contextExecutor(ctx));
                                return 1;
                            })
                            .build(),
                    "Use this command to show information whether you are currently linked.",
                    List.of("info"));
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
        getServer().motd(Components.MOTD);

        getServer().clearRecipes();
        getServer().setDefaultGameMode(GameMode.ADVENTURE);

        // Clean up every 10 seconds
        Bukkit.getScheduler().runTaskTimer(this, playerIdleCache::cleanUp, 0, 10 * 20);

        getLogger().info("Started Global Linking plugin!");
    }

    @EventHandler
    public void onCommands(PlayerCommandSendEvent event) {
        if (event.getPlayer().isOp()) {
            return;
        }

        var toRemove = new ArrayList<String>();
        for (String command : event.getCommands()) {
            if (!PERMITTED_COMMANDS.contains(command)) {
                toRemove.add(command);
            }
        }
        event.getCommands().removeAll(toRemove);
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

            if (!linkLookupService.isLookupCompleted(player)) {
                player.sendMessage(Components.LINK_INFO_UNAVAILABLE);
                return;
            }

            if (linkLookupService.isLinkedCached(player)) {
                player.sendMessage(Components.UNLINK_INSTRUCTION);
            } else {
                player.sendMessage(Components.LINK_INSTRUCTION);
            }
            return;
        }

        for (String permitted : PERMITTED_COMMANDS) {
            if (command.startsWith(permitted)) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.quitMessage(null);
        playerIdleCache.invalidate(event.getPlayer().getUniqueId());
        linkLookupService.invalidate(event.getPlayer());
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // todo player idle should not only depend on movement, also check for pending links
        int diffX = event.getFrom().getBlockX() - event.getTo().getBlockX();
        int diffY = event.getFrom().getBlockZ() - event.getTo().getBlockZ();
        if (Math.abs(diffX) > 0 || Math.abs(diffY) > 0) {
            playerIdleCache.put(event.getPlayer().getUniqueId(), Instant.now());
        }
    }
}
