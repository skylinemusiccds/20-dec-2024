package com.universe.audioflare.data.model.explore.mood.genre

import androidx.compose.runtime.Immutable
import com.universe.audioflare.data.model.searchResult.songs.Artist

@Immutable
data class ItemsSong(
    val title: String,
    val artist: List<Artist>?,
    val videoId: String,
)
