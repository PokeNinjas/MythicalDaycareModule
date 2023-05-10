package com.mythicalnetwork.mythicaldaycare.data

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class AspectChanceData(val aspect: String, val chance: Double) {
    companion object{
        val CODEC: Codec<AspectChanceData> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("aspect").forGetter { it.aspect },
                Codec.DOUBLE.fieldOf("chance").forGetter { it.chance }
            ).apply(instance, ::AspectChanceData)
        }
    }
}