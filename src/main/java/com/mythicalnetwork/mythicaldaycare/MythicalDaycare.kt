package com.mythicalnetwork.mythicaldaycare

import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import com.mythicalnetwork.mythicaldaycare.commands.DaycareCommand
import com.mythicalnetwork.mythicaldaycare.daycare.DaycareManager
import com.mythicalnetwork.mythicaldaycare.daycare.Egg
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance
import com.pokeninjas.kingdoms.fabric.Kingdoms
import dev.lightdream.lambda.ScheduleUtils
import net.minecraft.server.MinecraftServer
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.command.api.CommandRegistrationCallback
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents
import org.quiltmc.qsl.lifecycle.api.event.ServerTickEvents
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents
import org.slf4j.Logger


object MythicalDaycare : ModInitializer {

    const val MODID = "mythicaldaycare"
    val LOGGER: Logger = LogUtils.getLogger()

    var instance: MythicalDaycare? = null

    private var CURRENT_SERVER: MinecraftServer? = null

    var debugMode: Boolean = false

    val GSON = GsonBuilder()
        .registerTypeAdapter(PastureInstance::class.java, PastureInstance.Serializer())
        .registerTypeAdapter(PastureInstance::class.java, PastureInstance.Deserializer())
        .registerTypeAdapter(Egg::class.java, Egg.Serializer())
        .registerTypeAdapter(Egg::class.java, Egg.Deserializer())
        .create()

    val CONFIG: MythicalDaycareConfig = MythicalDaycareConfig.createAndLoad()

    override fun onInitialize(mod: ModContainer?) {
        println("Hello from MythicalDaycare!")
        instance = this
        ServerLifecycleEvents.READY.register {
            CURRENT_SERVER = it
        }
        ServerTickEvents.END.register {
            DaycareManager.INSTANCE.tick()
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            DaycareManager.INSTANCE.onPlayerJoin(handler.player)
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            DaycareCommand.register(dispatcher)
        }
        ScheduleUtils.runTaskTimerAsync({ task ->
            val instance = Kingdoms.getInstance() ?: return@runTaskTimerAsync
            instance.listenerManager.register(DaycareManager.INSTANCE)
            task.cancel()
        }, 0L, 1000L)
    }

    fun getCurrentServer(): MinecraftServer {
        return CURRENT_SERVER!!
    }

}