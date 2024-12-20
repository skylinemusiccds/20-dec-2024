package com.universe.audioflare.ui.component

import android.os.Bundle
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.universe.audioflare.R
import com.universe.audioflare.common.Config
import com.universe.audioflare.data.db.entities.AlbumEntity
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PlaylistEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.searchResult.playlists.PlaylistsResult
import com.universe.audioflare.data.type.LibraryType
import com.universe.audioflare.data.type.PlaylistType
import com.universe.audioflare.data.type.RecentlyType
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.service.QueueData
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.viewModel.LibraryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@UnstableApi
fun LibraryItem(
    state: LibraryItemState,
    viewModel: LibraryViewModel = koinInject(),
    navController: NavController
) {
    val context = LocalContext.current
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var songEntity by remember { mutableStateOf<SongEntity?>(null) }
    val title = when (state.type) {
        is LibraryItemType.YouTubePlaylist -> stringResource(R.string.your_youtube_playlists)
        is LibraryItemType.LocalPlaylist -> stringResource(R.string.your_playlists)
        is LibraryItemType.FavoritePlaylist -> stringResource(R.string.favorite_playlists)
        is LibraryItemType.DownloadedPlaylist -> stringResource(R.string.downloaded_playlists)
        is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
    }
    val noPlaylistTitle = when (state.type) {
        is LibraryItemType.YouTubePlaylist -> stringResource(R.string.no_YouTube_playlists)
        is LibraryItemType.LocalPlaylist -> stringResource(R.string.no_playlists_added)
        LibraryItemType.DownloadedPlaylist -> stringResource(R.string.no_playlists_downloaded)
        LibraryItemType.FavoritePlaylist -> stringResource(R.string.no_favorite_playlists)
        is LibraryItemType.RecentlyAdded -> stringResource(R.string.recently_added)
    }
    Box {
        if (showBottomSheet) {
            NowPlayingBottomSheet(
                onDismiss = {
                    showBottomSheet = false
                    songEntity = null
                },
                navController = navController,
                song = songEntity ?: return,
                onDelete = {

                }
            )
        }
        Column {
            Box(
                modifier = Modifier.padding(top = 15.dp, start = 10.dp, end = 10.dp),
            ) {
                Text(
                    text = title,
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .align(Alignment.CenterStart)
                )
                if (state.type is LibraryItemType.LocalPlaylist || state.type is LibraryItemType.YouTubePlaylist) {
                    TextButton(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                        onClick = {
                            if (state.type is LibraryItemType.LocalPlaylist) {
                                showAddSheet = true
                            } else {
                                (state.type as LibraryItemType.YouTubePlaylist).onReload.invoke()
                            }
                        }
                    ) {
                        if (state.type is LibraryItemType.LocalPlaylist) {
                            Text(stringResource(R.string.add))
                        } else {
                            Text(stringResource(R.string.reload))
                        }
                    }
                }
            }
            Crossfade(targetState = state.type is LibraryItemType.YouTubePlaylist && !state.type.isLoggedIn) { notLoggedIn ->
                if (notLoggedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.log_in_to_get_YouTube_playlist), style = typo.bodyMedium)
                    }
                } else {
                    Crossfade(targetState = state.isLoading, label = "Loading") { isLoading ->
                        if (!isLoading) {
                            if (state.type is LibraryItemType.RecentlyAdded) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    state.data.filterIsInstance<RecentlyType>().forEach { item ->
                                        when (item.objectType()) {
                                            RecentlyType.Type.SONG -> {
                                                SongFullWidthItems(
                                                    songEntity = item as SongEntity,
                                                    isPlaying = item.videoId == state.type.playingVideoId,
                                                    modifier = Modifier,
                                                    onMoreClickListener = {
                                                        songEntity = item
                                                        showBottomSheet = true
                                                    },
                                                    onClickListener = {
                                                        viewModel.setQueueData(
                                                            QueueData(
                                                                listTracks = arrayListOf(item.toTrack()),
                                                                firstPlayedTrack = item.toTrack(),
                                                                playlistId = "RDAMVM${item.videoId}",
                                                                playlistName = item.title,
                                                                playlistType = com.universe.audioflare.service.PlaylistType.RADIO,
                                                                continuation = null
                                                            )
                                                        )
                                                        viewModel.loadMediaItem(
                                                            item,
                                                            type = Config.SONG_CLICK,
                                                            index = 0
                                                        )
                                                    }
                                                )
                                            }
                                            RecentlyType.Type.ARTIST -> {
                                                ArtistFullWidthItems(
                                                    data = item as? ArtistEntity ?: return@forEach,
                                                    onClickListener = {
                                                        navController.navigateSafe(
                                                            R.id.action_global_artistFragment,
                                                            Bundle().apply {
                                                                putString("channelId", item.channelId)
                                                            }
                                                        )
                                                    }
                                                )
                                            }
                                            else -> {
                                                if (item is PlaylistType) {
                                                    PlaylistFullWidthItems(
                                                        data = item,
                                                        onClickListener = {
                                                            when (item) {
                                                                is AlbumEntity -> {
                                                                    navController.navigateSafe(
                                                                        R.id.action_global_albumFragment,
                                                                        Bundle().apply {
                                                                            putString("browseId", item.browseId)
                                                                        }
                                                                    )
                                                                }
                                                                is PlaylistEntity -> {
                                                                    navController.navigateSafe(
                                                                        R.id.action_global_playlistFragment,
                                                                        Bundle().apply {
                                                                            putString("id", item.id)
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (state.data.isNotEmpty()) {
                                    LazyRow {
                                        items(items = state.data) { item ->
                                            Box(modifier = Modifier.animateItem()) {
                                                HomeItemContentPlaylist(
                                                    onClick = {
                                                        when (item) {
                                                            is LocalPlaylistEntity -> {
                                                                navController.navigateSafe(
                                                                    R.id.action_bottom_navigation_item_library_to_localPlaylistFragment,
                                                                    Bundle().apply {
                                                                        putLong("id", item.id)
                                                                    }
                                                                )
                                                            }
                                                            is PlaylistsResult -> {
                                                                navController.navigateSafe(
                                                                    R.id.action_global_playlistFragment,
                                                                    Bundle().apply {
                                                                        putString("id", item.browseId)
                                                                    }
                                                                )
                                                            }
                                                            is AlbumEntity -> {
                                                                navController.navigateSafe(
                                                                    R.id.action_global_albumFragment,
                                                                    Bundle().apply {
                                                                        putString("browseId", item.browseId)
                                                                    }
                                                                )
                                                            }
                                                            is PlaylistEntity -> {
                                                                navController.navigateSafe(
                                                                    R.id.action_global_playlistFragment,
                                                                    Bundle().apply {
                                                                        putString("id", item.id)
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    },
                                                    data = item as? PlaylistType ?: return@items,
                                                    thumbSize = 125.dp
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp), contentAlignment = Alignment.Center
                                    ) {
                                        Text(noPlaylistTitle, style = typo.bodyMedium)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp), contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    if (showAddSheet) {
        var newTitle by remember { mutableStateOf(title) }
        val showAddSheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
            )
        val hideEditTitleBottomSheet: () -> Unit =
            { coroutineScope.launch {
                showAddSheetState.hide()
                showAddSheet = false
            } }
        ModalBottomSheet(
            onDismissRequest = { showAddSheet = false },
            sheetState = showAddSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = Color.Black.copy(alpha = .5f),
        ) {
            Card(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                        Modifier
                            .width(60.dp)
                            .height(4.dp),
                        colors =
                        CardDefaults.cardColors().copy(
                            containerColor = Color(0xFF474545),
                        ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { s -> newTitle = s },
                        label = {
                            Text(text = stringResource(id = R.string.playlist_name))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    TextButton(
                        onClick = {
                            if (newTitle.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.playlist_name_cannot_be_empty), Toast.LENGTH_SHORT).show()
                            } else {
                                (state.type as? LibraryItemType.LocalPlaylist)?.onAddClick?.invoke(newTitle)
                                hideEditTitleBottomSheet()
                            }
                        },
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                    ) {
                        Text(text = stringResource(id = R.string.create))
                    }
                }
            }
        }
    }
}

sealed class LibraryItemType {
    data class YouTubePlaylist(
        val isLoggedIn: Boolean,
        val onReload: () -> Unit = {}
    ) : LibraryItemType()

    data class LocalPlaylist(
        // Create new local playlist
        val onAddClick: (String) -> Unit
    ) : LibraryItemType()

    data object FavoritePlaylist: LibraryItemType()

    data object DownloadedPlaylist: LibraryItemType()

    data class RecentlyAdded(
        val playingVideoId: String
    ) : LibraryItemType()
}

data class LibraryItemState(
    val type: LibraryItemType,
    val data: List<LibraryType>,
    val isLoading: Boolean = true
)