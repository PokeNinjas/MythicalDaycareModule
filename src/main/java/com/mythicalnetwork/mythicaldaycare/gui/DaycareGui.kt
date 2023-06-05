package com.mythicalnetwork.mythicaldaycare.gui

import ca.landonjw.gooeylibs2.api.UIManager
import ca.landonjw.gooeylibs2.api.button.GooeyButton
import ca.landonjw.gooeylibs2.api.data.UpdateEmitter
import ca.landonjw.gooeylibs2.api.page.Page
import ca.landonjw.gooeylibs2.api.page.PageAction
import ca.landonjw.gooeylibs2.api.tasks.Task
import ca.landonjw.gooeylibs2.api.template.Template
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mythicalnetwork.mythicaldaycare.MythicalDaycare
import com.mythicalnetwork.mythicaldaycare.daycare.DaycareManager
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance
import com.mythicalnetwork.mythicaldaycare.utils.Utils
import com.mythicalnetwork.mythicaldaycare.utils.Utils.nullOrAddList
import com.mythicalnetwork.mythicaldaycare.utils.Utils.nullOrAddSingle
import com.mythicalnetwork.mythicaldaycare.utils.Utils.nullOrRun
import com.mythicalnetwork.mythicalmod.registry.MythicalItems
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStack.TooltipPart
import net.minecraft.world.item.Items


class DaycareGui(val player: ServerPlayer) :
    UpdateEmitter<Page>(), Page {
    private var template: ChestTemplate? = null
    var pasture: PastureInstance? = null
    var percentageBarTask: Task? = null

    init {
        val controller = DaycareGuiController()
        controller.subscribe(this, this::refresh)
        DaycareManager.INSTANCE.DAYCAREGUICONTROLLER[player.uuid] = controller

        template = ChestTemplate.Builder(3).build()
        refresh()
    }

    private fun refresh() {
        val user = MythicalDaycare.databaseManager.getUserOrCreate(player)
        pasture = DaycareManager.INSTANCE.getPasture(player)


        val firstPokemon: Pokemon? = pasture?.getLeftPokemon()
        val secondPokemon: Pokemon? = pasture?.getRightPokemon()

        template!!.set(1, 1, GooeyButton.builder()
            .display(nullOrRun(firstPokemon) {
                Utils.hideFlags(
                    Utils.pokemonToItem(firstPokemon),
                    TooltipPart.ADDITIONAL
                )
            })
            .title(nullOrAddSingle(firstPokemon) { Utils.colorOf("&a" + firstPokemon!!.displayName.string) })
            .lore(nullOrAddList(firstPokemon) { Utils.pokemonLore(firstPokemon) })
            .onClick { cons ->
                if (firstPokemon == null) {
                    TeamSelectGui(pasture, player)
                } else {
                    val leftPokemon: Pokemon = pasture!!.getLeftPokemon()!!
                    pasture!!.removeLeftPokemon()
                    DaycareManager.INSTANCE.setPasture(pasture!!, player)
                    Cobblemon.storage.getParty(player).add(leftPokemon)
                    player.playNotifySound(SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 2.0F, 1.0F)
                    refresh()
                }
            }
            .build()
        )
        template!!.set(1, 3, GooeyButton.builder()
            .display(nullOrRun(secondPokemon) {
                Utils.hideFlags(
                    Utils.pokemonToItem(secondPokemon),
                    TooltipPart.ADDITIONAL
                )
            })
            .title(nullOrAddSingle(secondPokemon) { Utils.colorOf("&a" + secondPokemon!!.displayName.string) })
            .lore(nullOrAddList(secondPokemon) { Utils.pokemonLore(secondPokemon) })
            .onClick { cons ->
                if (secondPokemon == null) {
                    TeamSelectGui(pasture, player)
                } else {
                    val rightPokemon: Pokemon = pasture!!.getRightPokemon()!!
                    pasture!!.removeRightPokemon()
                    DaycareManager.INSTANCE.setPasture(pasture!!, player)
                    Cobblemon.storage.getParty(player).add(rightPokemon)
                    player.playNotifySound(SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.PLAYERS, 2.0F, 1.0F)
                    refresh()
                }
            }
            .build()
        )

        if (pasture?.getLeftPokemon() != null && pasture?.getRightPokemon() != null) {
            if (!PastureInstance.checkCompatible(pasture?.getLeftPokemon(), pasture?.getRightPokemon())) {
                template!!.set(
                    1, 2, GooeyButton.builder()
                        .display(ItemStack(Items.BARRIER))
                        .title(Utils.colorOf("&cPasture is incompatible!"))
                        .build()
                )
            } else {
                template!!.set(1, 2, null)
            }
        } else {
            template!!.set(1, 2, null)
        }

        if (pasture?.getEgg() != null) {
            val button = GooeyButton.builder()

            button.display(MythicalItems.POKEMON_EGG.defaultInstance)
                .title(Utils.colorOf("&aPokemon Egg"))
                .onClick { cons ->
                    if (DaycareManager.INSTANCE.getPasture(player)?.getEgg() != null) {
                        if (DaycareManager.INSTANCE.getEggCountForPlayer(player.uuid) >= MythicalDaycare.CONFIG.maxEggsPerPlayer()) {
                            cons.template.getSlot(cons.slot).setButton(
                                GooeyButton.builder().display(ItemStack(Items.BARRIER))
                                    .title(Utils.colorOf("&cMaximum eggs reached")).build()
                            )
                            player.playNotifySound(SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.25F, 1.25F)
                            Task.builder()
                                .execute { task ->
                                    getTemplate().getSlot(cons.slot).setButton(button.build())
                                }
                                .delay(20)
                                .build()
                            return@onClick
                        }

                        pasture!!.takeEgg(player)
                        player.playNotifySound(SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 0.5F)
                        refresh()
                    }
                }
                .build()

            template!!.set(2, 7, button.build())
        } else {
            template!!.set(2, 7, null)
        }

        template!!.set(0, 7, GooeyButton.builder()
            .display(ItemStack(Items.PAPER).apply { orCreateTag.putInt("CustomModelData", 1000) })
            .title(Utils.colorOf("&aBackpack"))
            .onClick { cons ->
                UIManager.openUIForcefully(player, EggsGui(player))
            }
            .build())

        percentageBarTask?.setExpired()
        percentageBarTask = Task.builder()
            .execute { task -> setPercentageBar(pasture) }
            .infinite()
            .interval(MythicalDaycare.CONFIG.progressUpdateTime().toLong() * 20)
            .build()
    }

    // Method that will set the template percentage item with a specific CustomModelData number depending on the percentage. 100025 is 0% and 100035 is 100%
    fun setPercentageBar(savedInstance: PastureInstance?) {
        val tickInstance: PastureInstance? = DaycareManager.INSTANCE.PASTUREMAP[player.uuid]
        val itemStack = MythicalItems.PROGRESS_BAR.defaultInstance
        var percentage = 0.0

        if (savedInstance != null && savedInstance.isComplete()) {
            percentage = 10.0
        } else if (tickInstance != null && tickInstance.getRemainingSeconds() > 0) {
            percentage = (MythicalDaycare.CONFIG.breedingTime().toDouble() - tickInstance.getRemainingSeconds()) / MythicalDaycare.CONFIG.breedingTime()
        }

        template!!.set(
            2, 8, GooeyButton.builder()
                .display(itemStack.apply { orCreateTag.putDouble("progress", percentage) })
                .title(Utils.colorOf(""))
                .build()
        )
    }

    override fun onClose(action: PageAction) {
        DaycareManager.INSTANCE.DAYCAREGUICONTROLLER.remove(player.uuid)
        percentageBarTask?.setExpired()
    }

    override fun getTemplate(): Template {
        return template!!
    }

    override fun getTitle(): Component {
        return Component.literal(Utils.colorOf("&fꀽꐘ"))
    }

    companion object {
        class DaycareGuiController : UpdateEmitter<DaycareGui?>()
    }
}