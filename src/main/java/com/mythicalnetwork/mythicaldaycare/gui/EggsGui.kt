package com.mythicalnetwork.mythicaldaycare.gui

import ca.landonjw.gooeylibs2.api.UIManager
import ca.landonjw.gooeylibs2.api.button.GooeyButton
import ca.landonjw.gooeylibs2.api.data.UpdateEmitter
import ca.landonjw.gooeylibs2.api.page.Page
import ca.landonjw.gooeylibs2.api.template.Template
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate
import com.cobblemon.mod.common.Cobblemon
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

    init {
        val controller = EggsGuiController()
        controller.subscribe(this, this::refresh)
        DaycareManager.INSTANCE.EGGSGUICONTROLLER[player.uuid] = controller

        template = ChestTemplate.Builder(3).build()
        refresh()
    }

    private fun refresh() {
        val buttons: MutableList<GooeyButton> = mutableListOf()
        val user: DaycareUser? = MythicalDaycare.databaseManager.getUser(player)
        val eggs: List<Egg> = DaycareManager.INSTANCE.HATCHMAP[player.uuid] ?: emptyList()

        for (i in 0..MythicalDaycare.CONFIG.maxEggsPerPlayer()) {
            if (i >= eggs.size)
                break

            val egg: Egg = eggs[i]
            if (egg.isComplete()) {
                buttons.add(GooeyButton.builder()
                    .display(
                        Utils.hideFlags(
                            Utils.pokemonToItem(egg.getPokemon()),
                            ItemStack.TooltipPart.ADDITIONAL
                        )
                    )
                    .title(Utils.colorOf("&a" + egg.getPokemon().species.name))
                    .lore(Utils.pokemonLore(egg.getPokemon()))
                    .onClick { cons ->
                        Cobblemon.storage.getParty(player).add(egg.getPokemon())
                        DaycareManager.INSTANCE.removeEgg(player.uuid, egg.getSlot())
                        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F)
                        refresh()
                    }
                    .build())
            } else {
                buttons.add(
                    GooeyButton.builder()
                        .display(Utils.hideFlags(egg.getItemStack(), ItemStack.TooltipPart.ADDITIONAL))
                        .title(Utils.colorOf("&aPokemon Egg"))
                        .lore(listOf(Utils.colorOf("&dHatch Time: &f" + Utils.getFormattedTime((egg.getMaxTime() - egg.getTime()) / 20))))
                        .build()
                )
            }
        }
        template!!.rectangleFromList(1, 2, 2, 5, buttons as List<GooeyButton>)
        template!!.set(0, 0,
            GooeyButton.builder()
                .display(ItemStack(Items.PAPER).apply { orCreateTag.putInt("CustomModelData", 20001) })
                .title(Utils.colorOf("&cBack"))
                .onClick { cons ->
                    UIManager.openUIForcefully(player, DaycareGui(player))
                }
                .build())
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