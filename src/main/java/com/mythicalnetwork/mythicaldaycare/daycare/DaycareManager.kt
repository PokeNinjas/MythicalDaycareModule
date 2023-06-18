package com.mythicalnetwork.mythicaldaycare.daycare

import com.cobblemon.mod.common.pokemon.Pokemon
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.database.DaycareUser
import com.mythicalnetwork.mythicaldaycare.gui.DaycareGui
import com.mythicalnetwork.mythicaldaycare.gui.EggsGui
import com.mythicalnetwork.mythicaldaycare.utils.Utils
import com.pokeninjas.kingdoms.fabric.dto.database.impl.UserAPI
import eu.pb4.placeholders.api.PlaceholderContext
import eu.pb4.placeholders.api.Placeholders
import eu.pb4.placeholders.api.TextParserUtils
import eu.pb4.placeholders.api.node.TextNode
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import java.time.ZonedDateTime
import java.util.*
import java.util.function.Consumer

class DaycareManager {
    // Handles ticking pastures for egg laying
    var PASTUREMAP: MutableMap<UUID, PastureInstance> = mutableMapOf()

    // Handles ticking players eggs for hatching. Every player online should be present here for saving reasons. Egg list may just be empty, though never null
    var HATCHMAP: MutableMap<UUID, ArrayList<Egg>> = mutableMapOf()

    // Controls the Daycare GUI so that it can be updated when a egg is born
    var DAYCAREGUICONTROLLER: MutableMap<UUID, DaycareGui.Companion.DaycareGuiController> = mutableMapOf()

    // Controls the Eggs GUI so that it can be updated when a egg is hatched
    var EGGSGUICONTROLLER: MutableMap<UUID, EggsGui.Companion.EggsGuiController> = mutableMapOf()

    // Used for delaying the tick check for every 20 ticks
    private var checkTicks: Int = 0

    companion object {
        val INSTANCE: DaycareManager = DaycareManager()

        // Static method to handle when a pasture has completed an egg
        fun onPastureComplete(): Consumer<PastureInstance> {
            return Consumer<PastureInstance> { instance -> INSTANCE.handleEggSpawn(instance) }
        }

        // Static method to handle when an egg is taken from the daycare and should be added to the backpack
        fun onEggTaken(): Consumer<PastureInstance> {
            return Consumer<PastureInstance> { instance -> INSTANCE.handleEggTaken(instance) }
        }

        // Static method to handle when a egg has completed hatching
        fun onEggComplete(): Consumer<Egg> {
            return Consumer<Egg> { egg -> INSTANCE.handleEggHatch(egg) }
        }
    }

    fun getUserOrCreate(uuid: UUID): DaycareUser {
        return UserAPI.getData(uuid, "mythical_daycare_user", DaycareUser::class.java) ?: DaycareUser()
    }

    fun handleEggSpawn(instance: PastureInstance) {
        INSTANCE.PASTUREMAP.remove(instance.getPlayer())
        DAYCAREGUICONTROLLER[instance.getPlayer()].let { it?.update() }

        val player = MythicalDaycare.getCurrentServer().playerList.getPlayer(instance.getPlayer())
        if (player != null) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.5F, 0.5F)

            try {
                var message: TextNode? = TextParserUtils.formatNodes(MythicalDaycare.CONFIG.eggLaidMessage())
                val placeholderMap: HashMap<String, Component> = HashMap()
                placeholderMap["left_pokemon"] =
                    instance.getLeftPokemon()?.species?.name?.let { Component.literal(it) } ?: Component.empty()
                placeholderMap["right_pokemon"] =
                    instance.getRightPokemon()?.species?.name?.let { Component.literal(it) } ?: Component.empty()
                message = Placeholders.parseNodes(message, Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM, placeholderMap)
                player.sendSystemMessage(
                    (message!!.toText(PlaceholderContext.of(player).asParserContext(), true) as MutableComponent)
                        .withStyle { style ->
                            style
                                .withHoverEvent(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Placeholders.parseNodes(
                                            TextParserUtils.formatNodes(MythicalDaycare.CONFIG.daycareHoverMessage()),
                                            Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM,
                                            placeholderMap
                                        )
                                            .toText(PlaceholderContext.of(player).asParserContext(), true)
                                    )
                                )
                                .withClickEvent(
                                    ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/${MythicalDaycare.CONFIG.daycareCommand()}"
                                    )
                                )
                        })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun handleEggTaken(instance: PastureInstance) {
        if (instance.getEgg() != null) {
            val user: DaycareUser = getUserOrCreate(instance.getPlayer())
            // Get the currently ticking list of eggs from the Hatch Map
            val eggs: ArrayList<Egg> = ArrayList(INSTANCE.HATCHMAP[instance.getPlayer()] ?: emptyList())

            // Update the slot that the egg is within for future use
            instance.getEgg()?.setSlot(eggs.size)

            // Add to list and update data for hatching
            eggs.add(instance.getEgg()!!)
            user.setEggData(instance.getPlayer(), eggs)
            INSTANCE.HATCHMAP[instance.getPlayer()] = eggs
        }
        DAYCAREGUICONTROLLER[instance.getPlayer()].let { it?.update() }
    }

    fun handleEggHatch(egg: Egg) {
        INSTANCE.HATCHMAP[egg.getPlayer()]?.set(egg.getSlot(), egg)
        getUserOrCreate(egg.getPlayer()).setEggData(egg.getPlayer(), INSTANCE.HATCHMAP[egg.getPlayer()])
        EGGSGUICONTROLLER[egg.getPlayer()].let { it?.update() }

        val player: ServerPlayer? = MythicalDaycare.getCurrentServer().playerList.getPlayer(egg.getPlayer())
        if (player != null) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.5F, 0.5F)
            val placeholderMap: HashMap<String, Component> = HashMap()
            player.sendSystemMessage(
                (Placeholders.parseNodes(
                    TextParserUtils.formatNodes(MythicalDaycare.CONFIG.eggHatchedMessage()),
                    Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM,
                    placeholderMap
                )
                    .toText(PlaceholderContext.of(player).asParserContext(), true) as MutableComponent)
                    .withStyle { style ->
                        style
                            .withHoverEvent(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    Placeholders.parseNodes(
                                        TextParserUtils.formatNodes(MythicalDaycare.CONFIG.daycareHoverMessage()),
                                        Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM,
                                        placeholderMap
                                    )
                                        .toText(PlaceholderContext.of(player).asParserContext(), true)
                                )
                            )
                            .withClickEvent(
                                ClickEvent(
                                    ClickEvent.Action.RUN_COMMAND,
                                    "/${MythicalDaycare.CONFIG.daycareCommand()}"
                                )
                            )
                    })
        }
    }

    fun getPasture(player: ServerPlayer): PastureInstance? {
        val breedingUser = getUserOrCreate(player.uuid)
        return breedingUser.getPasture()
    }

    fun getEggCountForPlayer(player: UUID): Int {
        val user = getUserOrCreate(player)
        val eggs = user.getEggs() ?: return 0
        return eggs.size
    }

    fun getEggs(player: UUID): List<Egg> {
        val user = getUserOrCreate(player)
        return user.getEggs() ?: emptyList()
    }

    fun removeEgg(player: UUID, slot: Int) {
        // Remove them from the map
        val eggs = INSTANCE.HATCHMAP[player]
        eggs?.removeAt(slot)

        // Set the new list and reorder the slots, so they are correct
        INSTANCE.HATCHMAP[player] = eggs ?: ArrayList()
        for (i in 0 until (INSTANCE.HATCHMAP[player]?.size ?: 0)) {
            INSTANCE.HATCHMAP[player]!![i].setSlot(i)
        }

        getUserOrCreate(player).setEggData(player, eggs)
    }

    fun setPasture(leftPokemon: Pokemon?, rightPokemon: Pokemon?, player: ServerPlayer) {
        val instance: PastureInstance = PastureInstance.createInstance(leftPokemon, rightPokemon, player.uuid)
        getUserOrCreate(player.uuid).setPastureData(player.uuid, instance)

        // Add to TICK_MAP
        if (instance.getLeftPokemon() != null && instance.getRightPokemon() != null
            && PastureInstance.checkCompatible(instance.getLeftPokemon(), instance.getRightPokemon())
        ) {
            instance.setReadyTime(
                ZonedDateTime.now().plusSeconds(MythicalDaycare.CONFIG.breedingTime().toLong())
                    .format(Utils.dateFormatter)
            )
            INSTANCE.PASTUREMAP[player.uuid] = instance
        } else {
            INSTANCE.PASTUREMAP.remove(player.uuid)
        }
    }

    fun setPasture(instance: PastureInstance, player: ServerPlayer) {
        getUserOrCreate(player.uuid).setPastureData(player.uuid, instance)

        // Add to TICK_MAP
        if (instance.getLeftPokemon() != null && instance.getRightPokemon() != null
            && PastureInstance.checkCompatible(instance.getLeftPokemon(), instance.getRightPokemon())
        ) {
            INSTANCE.PASTUREMAP[player.uuid] = instance
        } else {
            INSTANCE.PASTUREMAP.remove(player.uuid)
        }
    }

    fun onPlayerJoin(player: ServerPlayer) {
        // Add user to the Egg ticking map so they are continually saved
        getUserOrCreate(player.uuid)
            .let { user ->
                INSTANCE.HATCHMAP[player.uuid] = user.getEggs() as ArrayList<Egg>
                if (user.pasture != null) {
                    val instance = user.getPasture()
                    if (!instance.isComplete() && instance.getLeftPokemon() != null && instance.getRightPokemon() != null
                        && PastureInstance.checkCompatible(instance.getLeftPokemon(), instance.getRightPokemon())
                    ) {
                        if (instance.getReadyTime().isNullOrEmpty()) {
                            instance.setReadyTime(
                                ZonedDateTime.now().plusSeconds(MythicalDaycare.CONFIG.breedingTime().toLong())
                                    .format(Utils.dateFormatter)
                            )
                        }
                        INSTANCE.PASTUREMAP[player.uuid] = instance
                    }
                }
            }
    }

    fun onPlayerQuit(player: ServerPlayer) {
        // Save player's Egg Data and remove them from the Hatching Map so they aren't ticked.
        getUserOrCreate(player.uuid).setEggData(player.uuid, HATCHMAP[player.uuid])

        INSTANCE.HATCHMAP.remove(player.uuid)
        INSTANCE.PASTUREMAP.remove(player.uuid)
    }

    fun tick() {
        checkTicks++
        if (checkTicks >= 20) {
            checkTicks = 0
            try {
                with(INSTANCE.PASTUREMAP.toMutableMap().iterator()) {
                    forEach {
                        if (it.value.getLeftPokemon() != null && it.value.getRightPokemon() != null) {
                            it.value.tick()
                        }
                    }
                }
            } catch (e: Exception) {
                MythicalDaycare.LOGGER.error("Error occurred while iterating Pasture tick map!")
                e.printStackTrace()
            }
            try {
                with(INSTANCE.HATCHMAP.toMutableMap().iterator()) {
                    forEach {
                        with(it.value.iterator()) {
                            forEach { egg ->
                                if (!egg.isComplete()) {
                                    egg.tick()
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                MythicalDaycare.LOGGER.error("Error occurred while iterating Hatch tick map!")
                e.printStackTrace()
            }
        }
    }
}