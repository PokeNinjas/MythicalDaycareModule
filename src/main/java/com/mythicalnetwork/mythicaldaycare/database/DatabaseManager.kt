package com.mythicalnetwork.mythicaldaycare.database

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.daycare.Egg
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance
import dev.lightdream.databasemanager.database.HibernateDatabaseManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.UUID

class DatabaseManager: HibernateDatabaseManager(MythicalDaycare) {
    companion object {
        lateinit var GSON: Gson

        fun getGson(): Gson {
            return GSON
        }
    }
    init {
        GSON = GsonBuilder()
            .registerTypeAdapter(PastureInstance::class.java, PastureInstance.Serializer())
            .registerTypeAdapter(PastureInstance::class.java, PastureInstance.Deserializer())
            .registerTypeAdapter(Egg::class.java, Egg.Serializer())
            .registerTypeAdapter(Egg::class.java, Egg.Deserializer())
            .create()
    }
    override fun getClasses(): MutableList<Class<*>> {
        return mutableListOf(DaycareUser::class.java)
    }

    fun getAllUsers(): List<DaycareUser> {
        return getAll(DaycareUser::class.java)
    }

    fun getUser(uuid: UUID): DaycareUser? {
        var query: Query<DaycareUser> = get(DaycareUser::class.java)
        query.query.where(query.builder.equal(query.root.get<String>("uuid"), uuid))
        var output: List<DaycareUser> = query.execute()
        if(output.isEmpty()) {
            return null
        }
        return output[0]
    }

    fun getUser(player: Player): DaycareUser? {
        return getUser(player.uuid)
    }

    fun getUserOrCreate(player: ServerPlayer): DaycareUser {
        val query: Query<DaycareUser> = get(DaycareUser::class.java)
        query.query.where(query.builder.equal(query.root.get<String>("uuid"), player.uuid))
        val output: List<DaycareUser> = query.execute()
        if(output.isEmpty()){
            val user: DaycareUser = DaycareUser(player)
            user.save()
            return user
        }
        return output[0]
    }

}