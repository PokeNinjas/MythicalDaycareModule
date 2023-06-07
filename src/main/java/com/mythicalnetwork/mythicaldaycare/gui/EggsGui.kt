package com.mythicalnetwork.mythicaldaycare.gui

import ca.landonjw.gooeylibs2.api.UIManager
import ca.landonjw.gooeylibs2.api.button.ButtonClick
import ca.landonjw.gooeylibs2.api.button.GooeyButton
import ca.landonjw.gooeylibs2.api.data.UpdateEmitter
import ca.landonjw.gooeylibs2.api.page.Page
import ca.landonjw.gooeylibs2.api.tasks.Task
import ca.landonjw.gooeylibs2.api.template.Template
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.database.DaycareUser
import com.mythicalnetwork.mythicaldaycare.daycare.DaycareManager
import com.mythicalnetwork.mythicaldaycare.daycare.Egg
import com.mythicalnetwork.mythicaldaycare.utils.Utils
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class EggsGui(var player: ServerPlayer) :
    UpdateEmitter<Page>(), Page {
    private var template: ChestTemplate? = null

    private var confirmSlot: Int = -1
    private var confirmTask: Task? = null

    init {
        val controller = EggsGuiController()
        controller.subscribe(this, this::refresh)
        DaycareManager.INSTANCE.EGGSGUICONTROLLER[player.uuid] = controller

        template = ChestTemplate.Builder(3).build()
        refresh()
    }

    private fun refresh() {
        val buttons: MutableList<GooeyButton> = mutableListOf()
        val user: DaycareUser = DaycareManager.INSTANCE.getUserOrCreate(player.uuid)
        val eggs: List<Egg> = DaycareManager.INSTANCE.HATCHMAP[player.uuid] ?: emptyList()

        for (i in 0..MythicalDaycare.CONFIG.maxEggsPerPlayer()) {
            if (i >= eggs.size)
                break
            val egg: Egg = eggs[i]
            if (egg.isComplete()) {
                if (egg.getSlot() == confirmSlot) {
                    buttons.add(GooeyButton.builder()
                        .display(ItemStack(Items.LIME_DYE))
                        .title(Utils.colorOf("&aClick to confirm release"))
                        .onClick { cons ->
                            confirmSlot = -1
                            DaycareManager.INSTANCE.removeEgg(player.uuid, egg.getSlot())
                            player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F)
                            refresh()
                        }
                        .build())
                } else {
                    buttons.add(GooeyButton.builder()
                        .display(
                            Utils.hideFlags(
                                Utils.pokemonToItem(egg.getPokemon()),
                                ItemStack.TooltipPart.ADDITIONAL
                            )
                        )
                        .title(Utils.colorOf("&a" + egg.getPokemon().species.name))
                        .lore(getCompletedLore(egg.getPokemon()))
                        .onClick { cons ->
                            if (cons.clickType == ButtonClick.LEFT_CLICK || cons.clickType == ButtonClick.SHIFT_LEFT_CLICK) {
                                Cobblemon.storage.getParty(player).add(egg.getPokemon())
                                DaycareManager.INSTANCE.removeEgg(player.uuid, egg.getSlot())
                                player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F)
                                refresh()
                            } else if (cons.clickType == ButtonClick.RIGHT_CLICK || cons.clickType == ButtonClick.SHIFT_RIGHT_CLICK) {
                                confirmSlot = egg.getSlot()
                                refresh()
                            }
                        }
                        .build())
                }
            } else {
                buttons.add(
                    GooeyButton.builder()
                        .display(Utils.hideFlags(egg.getItemStack(), ItemStack.TooltipPart.ADDITIONAL))
                        .title(Utils.colorOf("&aPokemon Egg"))
                        .lore(listOf(Utils.colorOf("&dHatch Time: &f" + Utils.getFormattedTime(egg.getRemainingSeconds()))))
                        .build()
                )
            }
        }

        template!!.rectangleFromList(1, 2, 2, 5, buttons as List<GooeyButton>)
        template!!.set(0, 0,
            GooeyButton.builder()
                .display(ItemStack(Items.ARROW))
                .title(Utils.colorOf("&cBack"))
                .onClick { cons ->
                    UIManager.openUIForcefully(player, DaycareGui(player))
                }
                .build())
    }

    private fun getCompletedLore(pokemon: Pokemon): List<String> {
        var lore = Utils.pokemonLore(pokemon)
        lore.add(Utils.colorOf(""))
        lore.add(Utils.colorOf("&aLeft-Click to claim"))
        lore.add(Utils.colorOf("&aRight-Click to release"))

        return lore
    }

    override fun getTemplate(): Template {
        return template!!
    }

    override fun getTitle(): Component {
        return Component.literal(Utils.colorOf("&fꀽꐚ"))
    }

    companion object {
        class EggsGuiController : UpdateEmitter<EggsGui?>()
    }
}