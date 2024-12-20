package com.universe.audioflare.viewModel.uiState

import androidx.compose.ui.graphics.Color
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.ui.theme.md_theme_dark_background
import com.universe.audioflare.viewModel.FilterState
import java.time.LocalDateTime

data class LocalPlaylistState(
    val id: Long,
    val title: String,
    val thumbnail: String? = null,
    val colors: List<Color> =
        listOf(
            Color.Black,
            md_theme_dark_background,
        ),
    val inLibrary: LocalDateTime? = null,
    val downloadState: Int = DownloadState.STATE_NOT_DOWNLOADED,
    val syncState: Int = LocalPlaylistEntity.YouTubeSyncState.NotSynced,
    val ytPlaylistId: String? = null,
    val trackCount: Int = 0,
    val page: Int = 0,
    val isLoadedFull: Boolean = false,
    val loadState: PlaylistLoadState = PlaylistLoadState.Loading,
    val filterState: FilterState = FilterState.OlderFirst,
    val suggestions: SuggestionSongs? = null,
) {
    sealed class SuggestionState {
        data object Loading : SuggestionState()

        data object Error : SuggestionState()

        data class Success(
            val suggestSongs: SuggestionSongs,
        ) : SuggestionState()
    }

    sealed class PlaylistLoadState {
        data object Loading : PlaylistLoadState()

        data object Error : PlaylistLoadState()

        data object Success : PlaylistLoadState()
    }

    data class SuggestionSongs(
        val reloadParams: String,
        val songs: List<Track>,
    )

    companion object {
        fun initial(): LocalPlaylistState =
            LocalPlaylistState(
                id = 0,
                title = "",
                thumbnail = null,
                inLibrary = null,
                downloadState = 0,
                syncState = 0,
                trackCount = 0,
            )
    }
}