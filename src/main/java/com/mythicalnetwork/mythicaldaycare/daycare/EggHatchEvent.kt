package com.mythicalnetwork.mythicaldaycare.daycare

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import java.util.UUID

fun interface EggHatchEvent {
    companion object {
        @JvmField
        val EVENT: Event<EggHatchEvent> =
            EventFactory.createArrayBacked(EggHatchEvent::class.java) { listeners ->
                EggHatchEvent { player, egg ->
                    for (listener in listeners) {
                        listener.hatch(player, egg)
                    }
                }
            }
    }

    fun hatch(player: UUID, egg: Egg)
}