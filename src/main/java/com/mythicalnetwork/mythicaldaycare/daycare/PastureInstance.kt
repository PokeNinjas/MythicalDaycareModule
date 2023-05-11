package com.mythicalnetwork.mythicaldaycare.daycare

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.pokemon.Pokemon
import com.google.gson.*
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.utils.EggUtils
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.TagParser
import net.minecraft.server.level.ServerPlayer
import java.lang.reflect.Type
import java.util.*
import java.util.function.Consumer

class PastureInstance (
    private var leftPokemon: Pokemon?,
    private var rightPokemon: Pokemon?,
    private var ticks: Int = 0,
    private var egg: Egg? = null,
    private val player: UUID
) {
    private val maxTime: Int = MythicalDaycare.CONFIG.breedingTime()

    companion object {
        fun createInstance(leftPokemon: Pokemon?, rightPokemon: Pokemon?, player: UUID): PastureInstance {
            return PastureInstance(leftPokemon, rightPokemon, 0, null, player)
        }

        fun load(compoundTag: CompoundTag): PastureInstance {
            // I'm sure there's a better Kotlin way of doing this?
            val leftPokemon =
                if (!compoundTag.getCompound("leftPokemon").isEmpty) Pokemon().loadFromNBT(compoundTag.getCompound("leftPokemon")) else null
            val rightPokemon =
                if (!compoundTag.getCompound("rightPokemon").isEmpty) Pokemon().loadFromNBT(compoundTag.getCompound("rightPokemon")) else null
            val ticks = compoundTag.getInt("ticks")
            val maxTime = compoundTag.getInt("maxTime")
            val player = compoundTag.getUUID("player")
            val egg = if (compoundTag.contains("egg")) {
                Egg.load(compoundTag.getCompound("egg"))
            } else {
                null
            }
            return PastureInstance(leftPokemon, rightPokemon, ticks, egg, player)
        }

        fun checkCompatible(leftPokemon: Pokemon?, rightPokemon: Pokemon?): Boolean {
            if (leftPokemon == null || rightPokemon == null)
                return false

            if (leftPokemon.species.eggGroups.contains(EggGroup.UNDISCOVERED) || rightPokemon.species.eggGroups.contains(
                    EggGroup.UNDISCOVERED
                )
            ) return false

            var areDifferentGender: Boolean =
                leftPokemon.gender != rightPokemon.gender || (leftPokemon.species == PokemonSpecies.getByName("ditto") || rightPokemon.species == PokemonSpecies.getByName(
                    "ditto"
                ))

            var areSameSpecies: Boolean =
                leftPokemon.species == rightPokemon.species || (leftPokemon.species == PokemonSpecies.getByName("ditto") || rightPokemon.species == PokemonSpecies.getByName(
                    "ditto"
                ))

            var shareEvolutionFamily: Boolean = leftPokemon!!.evolutions.toList().stream()
                .allMatch { evolution -> rightPokemon.evolutions.toList().contains(evolution) }

            var shareEggGroup: Boolean = leftPokemon.species.eggGroups.stream()
                .anyMatch { eggGroup -> rightPokemon.species.eggGroups.contains(eggGroup) }

            var areBothDitto: Boolean = leftPokemon.species == PokemonSpecies.getByName("ditto") && rightPokemon.species == PokemonSpecies.getByName("ditto")

            return (areDifferentGender && (areSameSpecies || shareEvolutionFamily || shareEggGroup) && !areBothDitto)
        }
    }

    fun save(): CompoundTag {
        val tag = CompoundTag()
        val leftPokemonTag = CompoundTag()
        val rightPokemonTag = CompoundTag()
        tag.put("leftPokemon", leftPokemon?.saveToNBT(leftPokemonTag) ?: CompoundTag())
        tag.put("rightPokemon", rightPokemon?.saveToNBT(rightPokemonTag) ?: CompoundTag())
        tag.putInt("ticks", ticks)
        tag.putInt("maxTime", maxTime)
        tag.putUUID("player", player)
        if (egg != null) {
            tag.put("egg", egg!!.save())
        }
        return tag
    }

    fun tick() {
        ticks++
        if (ticks >= maxTime) {
            val user = MythicalDaycare.databaseManager.getUser(player)
            egg = leftPokemon?.let { rightPokemon?.let { it1 -> EggUtils.generateEgg(player, it, it1) } }
            user!!.setPastureData(this)

            onComplete(DaycareManager.onPastureComplete())
        }
    }

    private fun onComplete(completeAction: Consumer<PastureInstance>) {
        completeAction.accept(this)
    }

    fun takeEgg(player: ServerPlayer) {
        onTakeEgg(DaycareManager.onEggTaken())

        // Update the pasture for a new egg
        egg = null
        ticks = 0
        MythicalDaycare.databaseManager.getUser(player)?.setPastureData(this)
        DaycareManager.INSTANCE.PASTUREMAP[player.uuid] = this
    }

    private fun onTakeEgg(takeAction: Consumer<PastureInstance>) {
        takeAction.accept(this)
    }

    fun checkCompatible(): Boolean {
        if (leftPokemon == null || rightPokemon == null)
            return false

        if (leftPokemon!!.species.eggGroups.contains(EggGroup.UNDISCOVERED) || rightPokemon!!.species.eggGroups.contains(
                EggGroup.UNDISCOVERED
            )
        ) return false

        var areDifferentGender: Boolean =
            leftPokemon!!.gender != rightPokemon!!.gender || (leftPokemon!!.species == PokemonSpecies.getByName("ditto") || rightPokemon!!.species == PokemonSpecies.getByName(
                "ditto"
            ))

        var areSameSpecies: Boolean =
            leftPokemon!!.species == rightPokemon!!.species || (leftPokemon!!.species == PokemonSpecies.getByName("ditto") || rightPokemon!!.species == PokemonSpecies.getByName(
                "ditto"
            ))

        var shareEvolutionFamily: Boolean = leftPokemon!!.evolutions.toList().stream()
            .allMatch { evolution -> rightPokemon!!.evolutions.toList().contains(evolution) }

        var shareEggGroup: Boolean = leftPokemon!!.species.eggGroups.stream()
            .anyMatch { eggGroup -> rightPokemon!!.species.eggGroups.contains(eggGroup) }

        return (areDifferentGender && (areSameSpecies || shareEvolutionFamily || shareEggGroup))
    }

    fun isComplete(): Boolean {
        return egg != null
    }

    fun getEgg(): Egg? {
        return egg
    }

    fun getTicks(): Int {
        return ticks
    }

    fun getMaxTicks(): Int {
        return maxTime
    }

    fun getLeftPokemon(): Pokemon? {
        return leftPokemon
    }

    fun getRightPokemon(): Pokemon? {
        return rightPokemon
    }

    fun setLeftPokemon(pokemon: Pokemon) {
        leftPokemon = pokemon
    }

    fun setRightPokemon(pokemon: Pokemon) {
        rightPokemon = pokemon
    }

    fun removeLeftPokemon() {
        leftPokemon = null
        ticks = 0
    }

    fun removeRightPokemon() {
        rightPokemon = null
        ticks = 0
    }

    fun getPlayer(): UUID {
        return player
    }

    override fun toString(): String {
        return "PastureInstance(leftPokemon=$leftPokemon, rightPokemon=$rightPokemon, ticks=$ticks, egg=$egg, player=$player, maxTime=$maxTime)"
    }


    class Serializer : JsonSerializer<PastureInstance> {
        override fun serialize(
            src: PastureInstance?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("serialized", src?.save().toString())
            return jsonObject
        }
    }

    class Deserializer : JsonDeserializer<PastureInstance> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): PastureInstance? {
            val jsonObject = json?.asJsonObject
            val compoundTag = jsonObject?.get("serialized")?.asString?.let { TagParser.parseTag(it) }
            return compoundTag?.let { load(it) }
        }
    }
}