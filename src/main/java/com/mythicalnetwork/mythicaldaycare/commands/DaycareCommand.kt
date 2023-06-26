package com.mythicalnetwork.mythicaldaycare.commands

import ca.landonjw.gooeylibs2.api.UIManager
import com.mojang.brigadier.CommandDispatcher
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.gui.DaycareGui
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object DaycareCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack?>) {
        dispatcher.register(
            Commands.literal("daycare")
                .then(Commands.literal("debug")
                    .requires { Permissions.check(it, "mythicaldaycare.command.daycare.debug") }
                    .executes { ctx ->
                        if (MythicalDaycare.debugMode) {
                            MythicalDaycare.debugMode = false
                            ctx.source.sendSystemMessage(Component.literal("Debug mode disabled!"))
                        } else {
                            MythicalDaycare.debugMode = true
                            ctx.source.sendSystemMessage(Component.literal("Debug mode enabled!"))
                        }

                        1
                    }
                )
                .requires { Permissions.check(it, "mythicaldaycare.command.daycare") }
                .executes { ctx ->
                    val player: ServerPlayer = ctx.source.player!!
                    UIManager.openUIForcefully(player, DaycareGui(player))

                    1
                }
        )
    }
}