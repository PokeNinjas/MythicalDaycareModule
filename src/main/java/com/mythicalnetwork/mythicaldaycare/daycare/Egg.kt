package com.mythicalnetwork.mythicaldaycare.daycare

import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.*
import com.mythicalnetwork.mythicaldaycare.utils.Utils
import com.mythicalnetwork.mythicalmod.registry.MythicalItems
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack
import java.lang.reflect.Type
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Consumer

class Egg(
    private var slot: Int,
    private val pokemon: Pokemon,
    private var readyTime: String? = null,
    private var isComplete: Boolean,
    private val player: UUID
) {
    private var checkTicks: Int = 0
    fun tick() {
        checkTicks++
        if (checkTicks >= 20) {
            checkTicks = 0
            if (isReady()) {
                isComplete = true
                onComplete(DaycareManager.onEggComplete())
            }
        }
    }

    private fun onComplete(completeAction: Consumer<Egg>) {
        completeAction.accept(this)
    }

    fun isReady(): Boolean {
        return !readyTime.isNullOrEmpty() && ZonedDateTime.parse(readyTime, Utils.dateFormatter)
            .isBefore(ZonedDateTime.now())
    }

    fun getRemainingSeconds(): Long {
        return if (readyTime == null) 0 else Duration.between(
            ZonedDateTime.now(),
            ZonedDateTime.parse(readyTime, Utils.dateFormatter)
        ).toSeconds()
    }

    fun getSlot(): Int {
        return slot
    }

    fun setSlot(slot: Int) {
        this.slot = slot
    }

    fun isComplete(): Boolean {
        return isComplete
    }

    fun getPokemon(): Pokemon {
        return pokemon
    }

    fun getReadyTime(): String? {
        return readyTime
    }

    fun getPlayer(): UUID {
        return player
    }

    fun setReadyTime(readyTime: String) {
        this.readyTime = readyTime
    }

    fun save(): CompoundTag {
        val tag: CompoundTag = CompoundTag()
        val pokeTag: CompoundTag = CompoundTag()
        pokemon.saveToNBT(pokeTag)
        tag.putInt("slot", slot)
        tag.put("pokemon", pokeTag)
        readyTime.orEmpty().let { tag.putString("readyTime", it) }
        tag.putBoolean("isComplete", isComplete)
        tag.putUUID("player", player)
        return tag
    }

    fun getItemStack(): ItemStack {
        return MythicalItems.POKEMON_EGG.defaultInstance
    }

    companion object {
        fun load(tag: CompoundTag): Egg {
            return Egg(
                tag.getInt("slot"),
                Pokemon().loadFromNBT(tag.getCompound("pokemon")),
                tag.getString("readyTime").ifEmpty { null },
                tag.getBoolean("isComplete"),
                tag.getUUID("player")
            )
        }
    }

    class Serializer : JsonSerializer<Egg> {
        override fun serialize(
            src: Egg?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("serialized", src?.save().toString())
            return jsonObject
        }
    }

    class Deserializer : JsonDeserializer<Egg> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): Egg? {
            val jsonObject = json?.asJsonObject
            val compoundTag = jsonObject?.get("serialized")?.asString?.let { TagParser.parseTag(it) }
            return compoundTag?.let { load(it) }
        }
    }
}