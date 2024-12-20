package com.universe.audioflare.ui.screen.library

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.universe.audioflare.R
import com.universe.audioflare.ui.component.EndOfPage
import com.universe.audioflare.ui.component.LibraryItem
import com.universe.audioflare.ui.component.LibraryItemState
import com.universe.audioflare.ui.component.LibraryItemType
import com.universe.audioflare.ui.component.LibraryTilingBox
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.utils.LocalResource
import com.universe.audioflare.viewModel.LibraryViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController
) {
    val loggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nowPlaying by viewModel.nowPlayingVideoId.collectAsState()
    val youTubePlaylist by viewModel.youTubePlaylist.collectAsState()
    val yourLocalPlaylist by viewModel.yourLocalPlaylist.collectAsState()
    val favoritePlaylist by viewModel.favoritePlaylist.collectAsState()
    val downloadedPlaylist by viewModel.downloadedPlaylist.collectAsState()
    val recentlyAdded by viewModel.recentlyAdded.collectAsState()
    LaunchedEffect(true) {
        Log.w("LibraryScreen", "Check youtubePlaylist: ${youTubePlaylist.data}")
        if (youTubePlaylist.data.isNullOrEmpty()) {
            viewModel.getYouTubePlaylist()
        }
        viewModel.getLocalPlaylist()
        viewModel.getPlaylistFavorite()
        viewModel.getDownloadedPlaylist()
        viewModel.getRecentlyAdded()
    }
    LaunchedEffect(nowPlaying) {
        Log.w("LibraryScreen", "Check nowPlaying: $nowPlaying")
        viewModel.getRecentlyAdded()
    }

    LazyColumn {
        item {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = typo.titleMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        }
        item {
            LibraryTilingBox(navController)
        }
        item {
            LibraryItem(
                state = LibraryItemState(
                    type = LibraryItemType.YouTubePlaylist(loggedIn) {
                        viewModel.getYouTubePlaylist()
                    },
                    data = youTubePlaylist.data ?: emptyList(),
                    isLoading = youTubePlaylist is LocalResource.Loading,
                ),
                navController = navController
            )
        }
        item {
            LibraryItem(
                state = LibraryItemState(
                    type = LibraryItemType.LocalPlaylist { newTitle ->
                        viewModel.createPlaylist(newTitle)
                    },
                    data = yourLocalPlaylist.data ?: emptyList(),
                    isLoading = yourLocalPlaylist is LocalResource.Loading,
                ),
                navController = navController
            )
        }
        item {
            LibraryItem(
                state = LibraryItemState(
                    type = LibraryItemType.FavoritePlaylist,
                    data = favoritePlaylist.data ?: emptyList(),
                    isLoading = favoritePlaylist is LocalResource.Loading,
                ),
                navController = navController
            )
        }
        item {
            LibraryItem(
                state = LibraryItemState(
                    type = LibraryItemType.DownloadedPlaylist,
                    data = downloadedPlaylist.data ?: emptyList(),
                    isLoading = downloadedPlaylist is LocalResource.Loading,
                ),
                navController = navController
            )
        }
        item {
            LibraryItem(
                state = LibraryItemState(
                    type = LibraryItemType.RecentlyAdded(
                        playingVideoId = nowPlaying
                    ), data = recentlyAdded.data ?: emptyList(), isLoading = recentlyAdded is LocalResource.Loading
                ),
                navController = navController
            )
        }
        item {
            EndOfPage()
        }
    }
}

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
fun LibraryScreenPreview() {
//    LibraryScreen()
}