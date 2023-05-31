package com.mythicalnetwork.mythicaldaycare

import com.mojang.logging.LogUtils
import com.mythicalnetwork.mythicaldaycare.commands.DaycareCommand
import com.mythicalnetwork.mythicaldaycare.database.DatabaseManager
import com.mythicalnetwork.mythicaldaycare.daycare.DaycareManager
import dev.lightdream.databasemanager.DatabaseMain
import dev.lightdream.databasemanager.config.SQLConfig
import dev.lightdream.databasemanager.database.HibernateDatabaseManager
import dev.lightdream.filemanager.FileManager
import dev.lightdream.filemanager.FileManagerMain
import dev.lightdream.logger.LoggableMain
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.minecraft.server.MinecraftServer
import org.quiltmc.loader.api.ModContainer
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer
import org.quiltmc.qsl.command.api.CommandRegistrationCallback
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents
import org.quiltmc.qsl.lifecycle.api.event.ServerTickEvents
import org.quiltmc.qsl.networking.api.ServerLoginConnectionEvents
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents
import org.reflections.Reflections
import org.slf4j.Logger
import java.io.File


/**
 * With Kotlin, the Entrypoint can be defined in numerous ways. This is showcased on Fabrics' Github:
 * https://github.com/FabricMC/fabric-language-kotlin#entrypoint-samples
 */
object MythicalDaycare : ModInitializer, DatabaseMain, LoggableMain, FileManagerMain {

    const val MODID = "mythicaldaycare"
    val LOGGER: Logger = LogUtils.getLogger()

    var instance: MythicalDaycare? = null

    var fileManager: FileManager? = null
    private var sqlConfig: SQLConfig? = null
    var databaseManagerObj: DatabaseManager? = null

    private var CURRENT_SERVER: MinecraftServer? = null
    val CONFIG: MythicalDaycareConfig = MythicalDaycareConfig.createAndLoad()

    override fun onInitialize(mod: ModContainer?) {
        println("Hello from MythicalDaycare!")
        instance = this
        dev.lightdream.logger.Logger.init(this)
        fileManager = FileManager(this)
        sqlConfig = fileManager!!.load(SQLConfig::class.java)
        databaseManagerObj = DatabaseManager()
        ServerLifecycleEvents.READY.register {
            CURRENT_SERVER = it
        }
        ServerTickEvents.END.register {
            DaycareManager.INSTANCE.tick()
        }
        ServerPlayConnectionEvents.JOIN.register{ handler, _, _ ->
            DaycareManager.INSTANCE.onPlayerJoin(handler.player)
        }
        ServerPlayConnectionEvents.DISCONNECT.register{ handler, _ ->
            DaycareManager.INSTANCE.onPlayerQuit(handler.player)
        }
        CommandRegistrationCallback.EVENT.register{ dispatcher, _, _ ->
            DaycareCommand.register(dispatcher)
        }
    }

    fun getCurrentServer(): MinecraftServer {
        return CURRENT_SERVER!!
    }

    override fun getDataFolder(): File {
        return File("${System.getProperty("user.dir")}/config/${MODID}")
    }

    override fun getPath(): String {
        return "${System.getProperty("user.dir")}/config/${MODID}"
    }

    override fun getSqlConfig(): SQLConfig {
        return sqlConfig!!
    }

    override fun getDatabaseManager(): DatabaseManager {
        return databaseManagerObj!!
    }

    fun getDatabaseManager(forJava: Boolean): DatabaseManager {
        return databaseManagerObj!!
    }

    override fun debugToConsole(): Boolean {
        return true // TODO: Make this configurable
    }


}