package com.mythicalnetwork.mythicaldaycare.daycare

import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.*
import com.mythicalnetwork.mythicalmod.registry.MythicalItems
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.world.item.ItemStack
import java.lang.reflect.Type
import java.util.UUID
import java.util.function.Consumer

class Egg(
    private var slot: Int,
    private val pokemon: Pokemon,
    private var seconds: Int,
    private val maxTime: Int,
    private var isComplete: Boolean,
    private val player: UUID
) {
    fun tick() {
        if (seconds < maxTime) {
            seconds++
        } else {
            isComplete = true
        }
    }

    private fun onComplete(completeAction: Consumer<Egg>) {
        completeAction.accept(this)
    }

    fun getSlot(): Int{
        return slot
    }

    fun setSlot(slot: Int){
        this.slot = slot
    }

    fun isComplete(): Boolean {
        return isComplete
    }

    fun getPokemon(): Pokemon {
        return pokemon
    }

    fun getTime(): Int {
        return seconds
    }

    fun getMaxTime(): Int{
        return maxTime
    }

    fun getPlayer(): UUID {
        return player
    }

    fun save(): CompoundTag {
        val tag: CompoundTag = CompoundTag()
        val pokeTag: CompoundTag = CompoundTag()
        pokemon.saveToNBT(pokeTag)
        tag.putInt("slot", slot)
        tag.put("pokemon", pokeTag)
        tag.putInt("seconds", seconds)
        tag.putInt("maxTime", maxTime)
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
                tag.getInt("seconds"),
                tag.getInt("maxTime"),
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