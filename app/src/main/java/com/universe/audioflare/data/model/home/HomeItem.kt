package com.universe.audioflare.data.model.home


import androidx.compose.runtime.Immutable
import com.google.gson.annotations.SerializedName
import com.universe.audioflare.data.model.searchResult.songs.Thumbnail

@Immutable
data class HomeItem(
    @SerializedName("contents")
    val contents: List<Content?>,
    @SerializedName("title")
    val title: String,
    val subtitle: String? = null,
    val thumbnail: List<Thumbnail>? = null,
    val channelId: String? = null,
)