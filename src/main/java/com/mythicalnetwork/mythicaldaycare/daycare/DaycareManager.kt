package com.mythicalnetwork.mythicaldaycare.daycare

import com.cobblemon.mod.common.pokemon.Pokemon
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.database.DaycareUser
import com.mythicalnetwork.mythicaldaycare.gui.DaycareGui
import com.mythicalnetwork.mythicaldaycare.gui.EggsGui
import com.mythicalnetwork.mythicaldaycare.utils.Utils
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

    fun handleEggSpawn(instance: PastureInstance) {
        INSTANCE.PASTUREMAP.remove(instance.getPlayer())
        DAYCAREGUICONTROLLER[instance.getPlayer()].let { it?.update() }

        val player = MythicalDaycare.getCurrentServer().playerList.getPlayer(instance.getPlayer())
        if (player != null) {
            player.playNotifySound(SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 0.5F, 0.5F)
            var message: TextNode? = TextParserUtils.formatNodes(MythicalDaycare.CONFIG.eggLaidMessage())
            val placeholderMap: HashMap<String, Component> = HashMap()
            placeholderMap["left_pokemon"] = Component.literal(instance.getLeftPokemon()!!.species.name)
            placeholderMap["right_pokemon"] = Component.literal(instance.getRightPokemon()!!.species.name)
            message = Placeholders.parseNodes(message, Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM, placeholderMap)
            player.sendSystemMessage(
                (message.toText(PlaceholderContext.of(player).asParserContext(), true) as MutableComponent)
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

    fun handleEggTaken(instance: PastureInstance) {
        if (instance.getEgg() != null) {
            val user: DaycareUser? = MythicalDaycare.databaseManager.getUser(instance.getPlayer())
            // Get the currently ticking list of eggs from the Hatch Map
            val eggs: ArrayList<Egg> = ArrayList(INSTANCE.HATCHMAP[instance.getPlayer()] ?: emptyList())

            // Update the slot that the egg is within for future use
            instance.getEgg()?.setSlot(eggs.size)

            // Add to list and update data for hatching
            eggs.add(instance.getEgg()!!)
            user?.setEggData(eggs)
            INSTANCE.HATCHMAP[instance.getPlayer()] = eggs
        }
        DAYCAREGUICONTROLLER[instance.getPlayer()].let { it?.update() }
    }

    fun handleEggHatch(egg: Egg) {
        INSTANCE.HATCHMAP[egg.getPlayer()]?.set(egg.getSlot(), egg)
        MythicalDaycare.databaseManager.getUser(egg.getPlayer())?.setEggData(INSTANCE.HATCHMAP[egg.getPlayer()])
        EGGSGUICONTROLLER[egg.getPlayer()].let { it?.update() }

        val player = MythicalDaycare.getCurrentServer().playerList.getPlayer(egg.getPlayer())
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
        val breedingUser = MythicalDaycare.databaseManager.getUserOrCreate(player)
        return breedingUser.getPasture()
    }

    fun getEggCountForPlayer(player: UUID): Int {
        val user = MythicalDaycare.databaseManager.getUser(player) ?: return 0
        val eggs = user.getEggs() ?: return 0
        return eggs.size
    }

    fun getEggs(player: UUID): List<Egg> {
        val user = MythicalDaycare.databaseManager.getUser(player)
        return user?.getEggs() ?: emptyList()
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

        MythicalDaycare.databaseManager.getUser(player)?.setEggData(eggs)
    }

    fun setPasture(leftPokemon: Pokemon?, rightPokemon: Pokemon?, player: ServerPlayer) {
        val instance: PastureInstance = PastureInstance.createInstance(leftPokemon, rightPokemon, player.uuid)
        MythicalDaycare.databaseManager.getUserOrCreate(player).let { user ->
            user.setPastureData(instance)
        }

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
        MythicalDaycare.databaseManager.getUserOrCreate(player)
            .let { user -> user.setPastureData(instance) }

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
        MythicalDaycare.databaseManager.getUserOrCreate(player)
            .let { user ->
                INSTANCE.HATCHMAP[player.uuid] = user.getEggs() as ArrayList<Egg>
                if (user.pasture != null) {
                    val instance = user.getPasture()
                    if (!instance.isComplete() && instance.getLeftPokemon() != null && instance.getRightPokemon() != null
                        && PastureInstance.checkCompatible(instance.getLeftPokemon(), instance.getRightPokemon())) {
                        INSTANCE.PASTUREMAP[user.uuid] = instance
                    }
                }
            }
    }

    fun onPlayerQuit(player: ServerPlayer) {
        // Save player's Egg Data and remove them from the Hatching Map so they aren't ticked.
        MythicalDaycare.databaseManager.getUserOrCreate(player)
            .let { user ->
                user.setEggData(HATCHMAP[player.uuid])
            }

        INSTANCE.HATCHMAP.remove(player.uuid)
        INSTANCE.PASTUREMAP.remove(player.uuid)
    }

    fun tick() {
        HashMap(INSTANCE.PASTUREMAP).values.forEach { entry ->
            if (entry.getLeftPokemon() != null && entry.getRightPokemon() != null) {
                entry.tick()
            }
        }
        HashMap(INSTANCE.HATCHMAP).values.forEach { list ->
            list.forEach { egg ->
                if (!egg.isComplete()) {
                    egg.tick()
                }
            }
        }
    }
}