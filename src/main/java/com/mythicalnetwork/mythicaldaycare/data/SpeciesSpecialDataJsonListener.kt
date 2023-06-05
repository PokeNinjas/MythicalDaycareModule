package com.mythicalnetwork.mythicaldaycare.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener
import net.minecraft.util.profiling.ProfilerFiller

class SpeciesSpecialDataJsonListener : SimpleJsonResourceReloadListener(GSON, "specials") {
    companion object {
        val GSON: Gson = Gson()
    }

    override fun apply(
        prepared: MutableMap<ResourceLocation, JsonElement>,
        manager: ResourceManager,
        profiler: ProfilerFiller
    ) {
        SpeciesSpecialData.SPECIAL_DATA.clear()
        prepared.forEach { (id, json) ->
            val data: DataResult<SpeciesSpecialData> = SpeciesSpecialData.CODEC.parse(JsonOps.INSTANCE, json)
            if (data.error().isPresent) {
                MythicalDaycare.LOGGER.error("Error parsing special data for $id: ${data.error().get()}")
                return@forEach
            }
            val dataHolder: SpeciesSpecialData = data.result().get()
            MythicalDaycare.LOGGER.info("Loaded special data for $id: ${dataHolder.species}")
            SpeciesSpecialData.SPECIAL_DATA[id.toString()] = dataHolder
        }
    }
}