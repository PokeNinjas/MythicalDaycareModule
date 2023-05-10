package com.mythicalnetwork.mythicaldaycare.utils

import com.cobblemon.mod.common.api.moves.BenchedMove
import com.cobblemon.mod.common.api.moves.Move
import com.cobblemon.mod.common.api.moves.MoveTemplate
import com.cobblemon.mod.common.api.pokeball.PokeBalls
import com.cobblemon.mod.common.api.pokemon.PokemonProperties
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureAssignments
import com.cobblemon.mod.common.api.pokemon.stats.Stat
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokeball.PokeBall
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.Species
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.data.AspectChanceData
import com.mythicalnetwork.mythicaldaycare.data.SpeciesSpecialData
import com.mythicalnetwork.mythicaldaycare.daycare.Egg
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance
import java.util.*

object EggUtils {
    fun generateEgg(player: UUID, leftPokemon: Pokemon, rightPokemon: Pokemon): Egg? {
        if (rightPokemon.species.eggGroups.contains(EggGroup.UNDISCOVERED) || leftPokemon.species.eggGroups.contains(
                EggGroup.UNDISCOVERED
            )
        ) return null
        var shareEggGroup: Boolean = rightPokemon.species.eggGroups.stream()
            .anyMatch { eggGroup -> leftPokemon.species.eggGroups.contains(eggGroup) }
        if (PastureInstance.checkCompatible(rightPokemon, leftPokemon)) {
            var nonDittoSpecies: Species =
                if (rightPokemon.species == PokemonSpecies.getByName("ditto")) leftPokemon.species else rightPokemon.species

            var otherSpeciesIfMaleIsntDitto: Species = if (leftPokemon.species != PokemonSpecies.getByName("ditto")) leftPokemon.species else rightPokemon.species

            var nonDittoPokemon: Pokemon =
                if (rightPokemon.species == PokemonSpecies.getByName("ditto")) leftPokemon else rightPokemon
            var hiddenAbility: Boolean =
                rightPokemon.aspects.contains("hiddenability") || leftPokemon.aspects.contains("hiddenability")
            var childPokemon: Pokemon = Pokemon()
            childPokemon.level = 1
            // if they are from the same egg group, randomize which parent's species is used if it is not ditto
            if (shareEggGroup && Math.random() < 0.5f) {
                childPokemon.species = nonDittoSpecies
            } else{
                childPokemon.species = nonDittoSpecies
            }
            childPokemon.species = if(shareEggGroup) {
                if(Math.random() > 0.5){
                    nonDittoSpecies
                } else {
                    otherSpeciesIfMaleIsntDitto
                }
            } else nonDittoSpecies
            while(childPokemon.preEvolution != null) {
                childPokemon.species = childPokemon.preEvolution!!.species
            }
            childPokemon.initialize()
            childPokemon = handleEggMoves(rightPokemon, leftPokemon, childPokemon)
            childPokemon = handleForm(childPokemon, rightPokemon, leftPokemon)
            childPokemon.caughtBall = isMasterOrCherish(handlePokeball(rightPokemon, leftPokemon))
            childPokemon = handleSpecials(childPokemon)
            handleIVs(childPokemon, rightPokemon, leftPokemon)
            handleFriendship(childPokemon)
            handleShiny(childPokemon)
            if (hiddenAbility && Math.random() < 0.4f) {
                PokemonProperties.parse("hiddenability").apply(childPokemon)
            }
            return Egg(-1, childPokemon, 0, MythicalDaycare.CONFIG.hatchTime(), false, player)
        }
        return null
    }

    private fun handleFriendship(childPokemon: Pokemon){
        childPokemon.setFriendship(120, true)
    }

    private fun handleShiny(childPokemon: Pokemon) {
        if(Math.random() < 1f / MythicalDaycare.CONFIG.shinyChance().toFloat()){
            PokemonProperties.parse("shiny=yes").apply(childPokemon)
        }
    }

    private fun handleIVs(childPokemon: Pokemon, leftPokemon: Pokemon, rightPokemon: Pokemon){
        val threeRandomStats: MutableList<Stat> = Stats.PERMANENT.toList().shuffled().subList(0, 3).toMutableList()
        for(i in threeRandomStats){
            if(Math.random() < 0.5f){
                leftPokemon.ivs[i]?.let { childPokemon.ivs[i] = it }
            } else {
                rightPokemon.ivs[i]?.let { childPokemon.ivs[i] = it }
            }
        }
    }

    private fun handlePokeball(leftPokemon: Pokemon, rightPokemon: Pokemon): PokeBall {
        return if(leftPokemon.species != rightPokemon.species){
            if(leftPokemon.species != PokemonSpecies.getByName("ditto")){
                leftPokemon.caughtBall
            } else {
                rightPokemon.caughtBall
            }
        } else {
            leftPokemon.caughtBall
        }
    }

    private fun isMasterOrCherish(ball: PokeBall): PokeBall {
        return if(ball == PokeBalls.MASTER_BALL || ball == PokeBalls.CHERISH_BALL){
            PokeBalls.POKE_BALL
        } else {
            ball
        }
    }

    private fun handleSpecials(childPokemon: Pokemon): Pokemon {
        for(s in arrayOf("all", childPokemon.species.name.lowercase().replace(" ", ""))){
            SpeciesSpecialData.getSpeciesData(s).stream().forEach { dataEntry: SpeciesSpecialData ->
                dataEntry.aspects.stream().forEach { aspect: AspectChanceData ->
                    if (SpeciesFeatureAssignments.getFeatures(childPokemon.species).contains(aspect.aspect)) {
                        if (Math.random() < aspect.chance) {
                            PokemonProperties.parse(aspect.aspect).apply(childPokemon)
                        }
                    }
                }
            }
        }
        return childPokemon
    }

    private fun handleEggMoves(
        leftPokemon: Pokemon,
        rightPokemon: Pokemon,
        childPokemon: Pokemon
    ): Pokemon {
        val leftHasEggMove: Boolean = leftPokemon.moveSet.getMoves().stream().anyMatch { move: Move ->
            leftPokemon.species.moves.eggMoves.contains(
                move.template
            )
        }
        val rightHasEggMove: Boolean = rightPokemon.moveSet.getMoves().stream().anyMatch { move: Move ->
            rightPokemon.species.moves.eggMoves.contains(
                move.template
            )
        }
        val eggMoves: MutableList<MoveTemplate> = mutableListOf()
        if (leftHasEggMove) {
            val leftEggMove: MoveTemplate = leftPokemon.moveSet.getMoves().stream().filter { move: Move ->
                leftPokemon.species.moves.eggMoves.contains(
                    move.template
                )
            }.findFirst().get().template
            eggMoves.add(leftEggMove)
        }
        if (rightHasEggMove) {
            val rightEggMove: MoveTemplate = rightPokemon.moveSet.getMoves().stream().filter { move: Move ->
                rightPokemon.species.moves.eggMoves.contains(
                    move.template
                )
            }.findFirst().get().template
            eggMoves.add(rightEggMove)
        }
        if (eggMoves.size > 0) {
            for (i in 0 until eggMoves.size.coerceAtMost(3)) {
                val move: MoveTemplate = eggMoves[i]
                if (childPokemon.moveSet.getMoves().size > i) {
                    val benchedMove: Move = childPokemon.moveSet.getMoves()[i]
                    childPokemon.benchedMoves.add(BenchedMove(benchedMove.template, benchedMove.raisedPpStages))
                }
                childPokemon.moveSet.setMove(i, Move(move, move.maxPp, 0))
            }
        }
        return childPokemon
    }

    private fun handleForm(childPokemon: Pokemon, femalePokemon: Pokemon, malePokemon: Pokemon): Pokemon {
        val femaleHasSpecialForm: Boolean = femalePokemon.form != femalePokemon.species.standardForm
        if (femaleHasSpecialForm) {
            childPokemon.form = femalePokemon.form
        } else {
            childPokemon.form = malePokemon.form
        }
        return childPokemon
    }
}