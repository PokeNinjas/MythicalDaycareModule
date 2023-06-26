package com.mythicalnetwork.mythicaldaycare.gui

import ca.landonjw.gooeylibs2.api.UIManager
import ca.landonjw.gooeylibs2.api.button.Button
import ca.landonjw.gooeylibs2.api.button.GooeyButton
import ca.landonjw.gooeylibs2.api.page.GooeyPage
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.pokemon.Pokemon
import com.mythicalnetwork.mythicaldaycare.daycare.DaycareManager
import com.mythicalnetwork.mythicaldaycare.daycare.PastureInstance
import com.mythicalnetwork.mythicaldaycare.utils.Utils
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.ItemStack

class TeamSelectGui(var currentInstance: PastureInstance?, var player: ServerPlayer) {

    init {
        refresh()
    }

    private fun refresh() {
        val template: ChestTemplate.Builder = ChestTemplate.Builder(3)

        val buttons: MutableList<GooeyButton> = mutableListOf()
        val party: PlayerPartyStore = Cobblemon.storage.getParty(player.uuid)

        for (i in 0..5) {
            val pokemon: Pokemon? = party.get(i)
            buttons.add(GooeyButton.builder()
                .display(
                    Utils.nullOrRunOther(
                        pokemon,
                        {
                            Utils.hideFlags(Utils.pokemonToItem(pokemon), ItemStack.TooltipPart.ADDITIONAL)
                        },
                        {
                            ItemStack(CobblemonItems.POKE_BALL.get())
                        }
                    )
                )
                .title(
                    Utils.nullOrOtherString(
                        pokemon,
                        Utils.colorOf("&aSlot " + (i + 1) + " - " + pokemon?.displayName?.string),
                        Utils.colorOf("&cEmpty Slot")
                    )
                )
                .lore(Utils.nullOrAddList(pokemon) { Utils.pokemonLore(pokemon) })
                .onClick { cons ->
                    if (pokemon != null && Cobblemon.storage.getParty(player).get(i) != null) {
                        if (currentInstance == null) {
                            currentInstance = PastureInstance.createInstance(null, null, player.uuid)
                        }
                        if (currentInstance!!.getLeftPokemon() == null) {
                            currentInstance!!.setLeftPokemon(pokemon)
                            Cobblemon.storage.getParty(player).remove(pokemon)
                        } else if (currentInstance!!.getRightPokemon() == null) {
                            currentInstance!!.setRightPokemon(pokemon)
                            Cobblemon.storage.getParty(player).remove(pokemon)
                        }
                        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.15F, 1.0F)
                        DaycareManager.INSTANCE.setPasture(
                            currentInstance!!.getLeftPokemon(),
                            currentInstance!!.getRightPokemon(),
                            player
                        )
                        UIManager.openUIForcefully(player, DaycareGui(player))
                    }
                }
                .build()
            )
        }
        template.rectangleFromList(1, 3, 2, 3, buttons as List<Button>)
        UIManager.openUIForcefully(
            player,
            GooeyPage.builder().template(template.build()).title(Utils.colorOf("&fꀽꐙ")).build()
        )
    }
}