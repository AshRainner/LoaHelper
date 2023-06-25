package com.lostark.dto.characters


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Item(
    @SerializedName("Description")
    val description: String,
    @SerializedName("Name")
    val name: String
): Serializable