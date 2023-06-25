package com.lostark.dto.characters


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ArmoryGem(
    @SerializedName("Effects")
    val effects: List<EffectXX>,
    @SerializedName("Gems")
    val gems: List<Gem>
): Serializable