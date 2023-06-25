package com.lostark.dto.characters


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ArmoryEngraving(
    @SerializedName("Effects")
    val effects: List<EffectX>,
    @SerializedName("Engravings")
    val engravings: List<Engraving>
): Serializable