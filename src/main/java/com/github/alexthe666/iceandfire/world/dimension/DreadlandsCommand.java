package com.github.alexthe666.iceandfire.world.dimension;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug commands for testing the Dreadlands dimension.
 *
 * Commands:
 *   /dreadlands portals <true|false>  — Toggle portal activation
 *   /dreadlands tp                     — Teleport to Dreadlands at current XZ
 *   /dreadlands status                 — Show portal state and boss fight flag
 *
 * Registration: Call DreadlandsCommand.register(dispatcher) from a
 * RegisterCommandsEvent handler.
 *
 * REMOVE OR GATE BEHIND A DEBUG CONFIG BEFORE RELEASE.
 */
public class DreadlandsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dreadlands")
                        .requires(source -> source.hasPermission(2)) // op only

                        .then(Commands.literal("portals")
                                .then(Commands.argument("active", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean active = BoolArgumentType.getBool(ctx, "active");
                                            MinecraftServer server = ctx.getSource().getServer();
                                            if (active) {
                                                DreadlandsPortalManager.reactivateAllPortals(server);
                                                ctx.getSource().sendSuccess(
                                                        new TextComponent("Dreadlands portals ACTIVATED"), true);
                                            } else {
                                                DreadlandsPortalManager.deactivateAllPortals(server);
                                                ctx.getSource().sendSuccess(
                                                        new TextComponent("Dreadlands portals DEACTIVATED"), true);
                                            }
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("tp")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    MinecraftServer server = ctx.getSource().getServer();
                                    ServerLevel dreadlands = server.getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);

                                    if (dreadlands == null) {
                                        ctx.getSource().sendFailure(
                                                new TextComponent("Dreadlands dimension not loaded!"));
                                        return 0;
                                    }

                                    if (player.level.dimension() == IafDimensionRegistry.DREADLANDS_LEVEL) {
                                        // Already in Dreadlands → go to overworld
                                        ServerLevel overworld = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                                        if (overworld != null) {
                                            player.changeDimension(overworld, new DreadlandsTeleporter(overworld));
                                            ctx.getSource().sendSuccess(
                                                    new TextComponent("Teleported to Overworld"), true);
                                        }
                                    } else {
                                        // Go to Dreadlands
                                        player.changeDimension(dreadlands, new DreadlandsTeleporter(dreadlands));
                                        ctx.getSource().sendSuccess(
                                                new TextComponent("Teleported to Dreadlands"), true);
                                    }
                                    return 1;
                                })
                        )

                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    ServerLevel dreadlands = server.getLevel(IafDimensionRegistry.DREADLANDS_LEVEL);

                                    if (dreadlands == null) {
                                        ctx.getSource().sendSuccess(
                                                new TextComponent("Dreadlands: NOT LOADED"), false);
                                        return 1;
                                    }

                                    DreadlandsPortalData data = DreadlandsPortalData.get(dreadlands);
                                    String status = String.format(
                                            "Dreadlands — Portals: %s | Boss fight: %s",
                                            data.arePortalsActive() ? "ACTIVE" : "DEACTIVATED",
                                            data.isBossFightActive() ? "IN PROGRESS" : "inactive"
                                    );
                                    ctx.getSource().sendSuccess(new TextComponent(status), false);
                                    return 1;
                                })
                        )
        );
    }
}