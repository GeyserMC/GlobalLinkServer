/*
 * Copyright (c) 2025 GeyserMC
 * Licensed under the MIT license
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */
package org.geysermc.globallinkserver;

import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class Components {
    private Components() {}

    private static final String PLATFORM_BEDROCK = "Bedrock";
    private static final String PLATFORM_JAVA = "Java";

    public static final Component MOTD = Component.text("GeyserMC ", NamedTextColor.GREEN)
            .append(Component.text("Link ", NamedTextColor.AQUA))
            .append(Component.text("Server", NamedTextColor.WHITE));

    public static final Component LINK_INSTRUCTION = Component.text(
                    "You are not linked. To link, run the ", NamedTextColor.AQUA)
            .append(Component.text("`/link`", NamedTextColor.GREEN))
            .append(Component.text(" command.", NamedTextColor.AQUA));

    public static final Component UNLINK_INSTRUCTION = Component.text(
                    "You are currently linked. To unlink, use ", NamedTextColor.AQUA)
            .append(Component.text("`/unlink`", NamedTextColor.RED))
            .append(Component.text("."));

    public static final Component LINK_INFO_UNAVAILABLE =
            Component.text("Your linking information is currently unavailable. Please wait!", NamedTextColor.RED);
    public static final Component KICK_IDLE = Component.text("You have been idle for too long!");
    public static final Component LINK_ALREADY_LINKED = Component.text(
            "You are already linked! You need to unlink first before linking again.", NamedTextColor.RED);
    public static final Component LINK_CODE_INVALID_RANGE = Component.text("Invalid link code!", NamedTextColor.RED);
    public static final Component LINK_REQUEST_REPLACED = Component.text(
            "You already had an active link request, so your old request has been invalidated.", NamedTextColor.AQUA);
    public static final Component LINK_REQUEST_NOT_FOUND =
            Component.text("Could not find the provided link. Has it expired?", NamedTextColor.RED);
    public static final Component LINK_REQUEST_SAME_PLATFORM = Component.text(
                    "You can only link a Java account to a Bedrock account. ", NamedTextColor.RED)
            .append(Component.text("Try to start the linking process again!"));
    public static final Component LINK_CREATE_ERROR = Component.text(
            "An unknown error occurred while linking your account. Try it again later!", NamedTextColor.RED);
    public static final Component LINK_CREATE_SUCCESS =
            Component.text("You are now successfully linked! :)", NamedTextColor.GREEN);

    public static final Component UNLINK_NOT_LINKED =
            Component.text("You are not linked to any account!", NamedTextColor.RED);
    public static final Component UNLINK_ERROR = Component.text(
            "An unknown error occurred while unlinking your account. Try it again later!", NamedTextColor.RED);
    public static final Component UNLINK_SUCCESS =
            Component.text("You are successfully unlinked.", NamedTextColor.GREEN);

    public static final Component INFO_NOT_LINKED = UNLINK_NOT_LINKED.color(NamedTextColor.AQUA);
    public static final Component INFO_UNAVAILABLE = Component.text("Failed to find current link!", NamedTextColor.RED);

    public static Component linkStarted(String otherPlatform, String code) {
        return Component.text("Please join on %s and run ".formatted(otherPlatform), NamedTextColor.GREEN)
                .append(Component.text("`/link " + code + "`", NamedTextColor.AQUA));
    }

    public static Component infoLinkInfo(String username, UUID uuid, boolean bedrock) {
        return Component.text(
                String.format(
                        "You are currently linked to the %s player %s (%s).",
                        bedrock ? PLATFORM_BEDROCK : PLATFORM_JAVA, username, uuid),
                NamedTextColor.GREEN);
    }

    public static Component cleanupLinkRequestExpired(int code) {
        return Component.text(
                "Your link (%s) has expired! Run the link account again if you need a new code.".formatted(code),
                NamedTextColor.RED);
    }
}
