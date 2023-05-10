package com.mythicalnetwork.mythicaldaycare.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class SpeciesSpecialData(
    val species: String,
    val aspects: List<AspectChanceData>
) {
    companion object {
        val SPECIAL_DATA: MutableMap<String, SpeciesSpecialData> = mutableMapOf()
        val CODEC: Codec<SpeciesSpecialData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("species").forGetter { it.species },
                AspectChanceData.CODEC.listOf().fieldOf("aspects").forGetter { it.aspects }
            ).apply(instance) { species, aspects ->
                SpeciesSpecialData(
                    species,
                    aspects
                ) }
        }
        fun getSpeciesData(species: String): List<SpeciesSpecialData> {
            return SPECIAL_DATA.filter { it.value.species == species }.values.toList()
        }
    }
}