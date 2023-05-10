package com.mythicalnetwork.mythicaldaycare

import io.wispforest.owo.config.annotation.Config
import io.wispforest.owo.config.annotation.Modmenu

@Modmenu(modId = "mythicaldaycare")
@Config(name = "mythical-daycare", wrapperName = "MythicalDaycareConfig")
class MythicalDaycareConfigModel {
    @JvmField
    var breedingTime: Int = 300
    @JvmField
    var hatchTime: Int = 300
    @JvmField
    var maxEggsPerPlayer: Int = 10
    @JvmField
    var shinyChance: Int = 8192
    @JvmField
    var progressUpdateTime: Int = 200
    @JvmField
    var eggLaidMessage: String = ""
    @JvmField
    var eggHatchedMessage: String = ""
    @JvmField
    var daycareHoverMessage: String = "<gold>Click to open the daycare!"
    @JvmField
    var daycareCommand: String = "daycare"
    @JvmField
    var sendLaidMessage: Boolean = true
    @JvmField
    var sendHatchedMessage: Boolean = true
}