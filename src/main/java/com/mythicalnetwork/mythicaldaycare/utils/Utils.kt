package com.mythicalnetwork.mythicaldaycare.utils

import com.cobblemon.mod.common.CobblemonItems
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.item.PokemonItem
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

object Utils {

    val dateFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    val pattern = Pattern.compile("#([A-Fa-f0-9]{6})")

    fun hideFlags(itemStack: ItemStack, vararg parts: ItemStack.TooltipPart): ItemStack {
        for (part in parts) itemStack.hideTooltipPart(part)
        return itemStack
    }

    fun getPercentage(max: Int, amount: Int): Int {
        return if (amount <= 0) 0 else (amount.toDouble() / max * 100).toInt()
    }

    fun pokemonToItem(pokemon: Pokemon?): ItemStack {
        return PokemonItem.from(pokemon!!, 1)
    }

    fun nullOrRun(obj: Any?, func: () -> ItemStack): ItemStack {
        if (obj != null) {
            return func()
        }
        // + sign icon!
        return ItemStack(CobblemonItems.POKE_BALL.get())
    }

    fun nullOrRunOther(obj: Any?, main: () -> ItemStack, other: () -> ItemStack): ItemStack {
        if (obj != null) {
            return main()
        }
        return other()
    }

    fun nullOrOther(obj: Any?, main: ItemStack, other: ItemStack): ItemStack {
        if (obj != null) {
            return main
        }
        return other
    }

    fun nullOrAddSingle(obj: Any?, func: () -> String): String {
        if (obj != null) {
            return func()
        }
        return colorOf("&cAdd a Pokemon")
    }

    fun nullOrAddList(obj: Any?, func: () -> List<String>): List<String> {
        if (obj != null) {
            return func()
        }
        return listOf("")
    }

    fun nullOrOtherString(obj: Any?, main: String, other: String): String {
        if (obj != null) {
            return main
        }
        return other
    }

    fun pokemonLore(pokemon: Pokemon?): MutableList<String> {
        pokemon ?: return ArrayList()
        val lore: MutableList<String> = ArrayList()
        lore.add(colorOf("&dLevel: &f" + pokemon.level))
        lore.add(colorOf("&dGender: &f" + titleCase(pokemon.gender.toString())))
        lore.add(colorOf("&dNature: &f" + titleCase(pokemon.nature.name.path)))
        lore.add(colorOf("&dAbility: &f" + titleCase(pokemon.ability.name)))
        lore.add(
            colorOf(
                "&dBall: &f" + titleCase(
                    pokemon.caughtBall.name.path.replace("_", " ")
                )
            )
        )
        lore.add(colorOf("&dHeld Item: &f" + if (pokemon.heldItem().item.equals(Items.AIR)) "None" else pokemon.heldItem().hoverName.string))
        lore.add("")
        val ivs: Int = Arrays.stream(Stats.values()).mapToInt { stat ->
            Objects.requireNonNullElse(
                pokemon.ivs[stat], 0
            )!!
        }.sum()
        lore.add(colorOf("&dIVs: &f" + ivs + "/186 &7(" + getPercentage(186, ivs) + "%)"))
        lore.add(
            colorOf(
                "&dHP: &f" + Objects.requireNonNullElse(
                    pokemon.ivs[Stats.HP],
                    0
                ) + " &7/ &dAtk: &f" + Objects.requireNonNullElse(
                    pokemon.ivs[Stats.ATTACK],
                    0
                ) + " &7/ &dDef: &f" + Objects.requireNonNullElse(pokemon.ivs[Stats.DEFENCE], 0)
            )
        )
        lore.add(
            colorOf(
                "&dSpA: &f" + Objects.requireNonNullElse(
                    pokemon.ivs[Stats.SPECIAL_ATTACK],
                    0
                ) + " &7/ &dSpD: &f" + Objects.requireNonNullElse(
                    pokemon.ivs[Stats.SPECIAL_DEFENCE],
                    0
                ) + " &7/ &dSpe: &f" + Objects.requireNonNullElse(pokemon.ivs[Stats.SPEED], 0)
            )
        )
        lore.add("")
        val evs: Int = Arrays.stream(Stats.values()).mapToInt { stat ->
            Objects.requireNonNullElse(
                pokemon.evs[stat], 0
            )!!
        }.sum()
        lore.add(colorOf("&dEVs: &f" + evs + "/510 &7(" + getPercentage(510, evs) + "%)"))
        lore.add(
            colorOf(
                "&dHP: &f" + Objects.requireNonNullElse(
                    pokemon.evs[Stats.HP],
                    0
                ) + " &7/ &dAtk: &f" + Objects.requireNonNullElse(
                    pokemon.evs[Stats.ATTACK],
                    0
                ) + " &7/ &dDef: &f" + Objects.requireNonNullElse(pokemon.evs[Stats.DEFENCE], 0)
            )
        )
        lore.add(
            colorOf(
                "&dSpA: &f" + Objects.requireNonNullElse(
                    pokemon.evs[Stats.SPECIAL_ATTACK],
                    0
                ) + " &7/ &dSpD: &f" + Objects.requireNonNullElse(
                    pokemon.evs[Stats.SPECIAL_DEFENCE],
                    0
                ) + " &7/ &dSpe: &f" + Objects.requireNonNullElse(pokemon.evs[Stats.SPEED], 0)
            )
        )
        lore.add("")
        lore.add(colorOf("&dMoves:"))
        var moves = StringBuilder()
        for (i in 0..3) {
            moves.append("&f").append(
                if (pokemon.moveSet[i] == null) "None" else titleCase(
                    pokemon.moveSet[i]?.name
                )
            )
            if (i == 1) {
                lore.add(colorOf(moves.toString()))
                moves = StringBuilder()
            } else if (i < 3) moves.append(" &7/ ")
        }
        lore.add(colorOf(moves.toString()))

        return lore
    }

    fun colorOf(text: String): String {
        return colorCodes(formatHex(text))
    }

    fun colorCodes(colorCodes: String): String {
        return Regex("&([A-Fa-f0-9k-oK-oRr])").replace(colorCodes, "§$1")
    }

    fun formatHex(text: String): String {
        val buf = StringBuffer()
        val matcher: Matcher = pattern.matcher(text)
        while (matcher.find()) {
            if (matcher.start(1) != -1) {
                matcher.appendReplacement(buf, "§#" + matcher.group(1))
            }
        }
        matcher.appendTail(buf)
        return buf.toString()
    }

    fun titleCase(input: String?): String? {
        if (input == null) return null
        val pattern: Pattern = Pattern.compile("\\b([a-zÀ-ÖØ-öø-ÿ])([\\w]*)")
        val matcher: Matcher = pattern.matcher(input.lowercase(Locale.getDefault()))
        val buffer = java.lang.StringBuilder()
        while (matcher.find()) matcher.appendReplacement(
            buffer,
            matcher.group(1).uppercase(Locale.getDefault()) + matcher.group(2)
        )
        return matcher.appendTail(buffer).toString()
    }


    fun getFormattedTime(time: Long): String {
        if (time <= 0) return "0"
        val timeFormatted: MutableList<String> = ArrayList()
        val days = time / 86400
        val hours = time % 86400 / 3600
        val minutes = time % 86400 % 3600 / 60
        val seconds = time % 86400 % 3600 % 60
        if (days > 0) {
            timeFormatted.add(days.toString() + "d")
        }
        if (hours > 0) {
            timeFormatted.add(hours.toString() + "h")
        }
        if (minutes > 0) {
            timeFormatted.add(minutes.toString() + "m")
        }
        if (seconds > 0) {
            timeFormatted.add(seconds.toString() + "s")
        }
        return timeFormatted.joinToString(" ")
    }
}