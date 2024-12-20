package com.universe.audioflare.ui.screen.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.universe.kotlinytmusicscraper.config.Constants
import com.universe.audioflare.R
import com.universe.audioflare.common.CHART_SUPPORTED_COUNTRY
import com.universe.audioflare.common.Config
import com.universe.audioflare.common.LIMIT_CACHE_SIZE.data
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.explore.mood.Mood
import com.universe.audioflare.data.model.home.HomeItem
import com.universe.audioflare.data.model.home.chart.Chart
import com.universe.audioflare.data.model.home.chart.toTrack
import com.universe.audioflare.extension.isScrollingUp
import com.universe.audioflare.extension.navigateSafe
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.service.PlaylistType
import com.universe.audioflare.service.QueueData
import com.universe.audioflare.ui.component.CenterLoadingBox
import com.universe.audioflare.ui.component.Chip
import com.universe.audioflare.ui.component.DropdownButton
import com.universe.audioflare.ui.component.EndOfPage
import com.universe.audioflare.ui.component.HomeItem
import com.universe.audioflare.ui.component.HomeShimmer
import com.universe.audioflare.ui.component.ItemArtistChart
import com.universe.audioflare.ui.component.ItemTrackChart
import com.universe.audioflare.ui.component.ItemVideoChart
import com.universe.audioflare.ui.component.MoodMomentAndGenreHomeItem
import com.universe.audioflare.ui.component.QuickPicksItem
import com.universe.audioflare.ui.component.RippleIconButton
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.viewModel.HomeViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@UnstableApi
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    sharedViewModel: SharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val accountInfo by viewModel.accountInfo.collectAsState()
    val homeData by viewModel.homeItemList.collectAsState()
    val newRelease by viewModel.newRelease.collectAsState()
    val chart by viewModel.chart.collectAsState()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsState()
    val chartLoading by viewModel.loadingChart.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var accountShow by rememberSaveable {
        mutableStateOf(false)
    }
    val regionChart by viewModel.regionCodeChart.collectAsState()
    val homeRefresh by sharedViewModel.homeRefresh.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsState()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsState()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.getHomeItemList()
        Log.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = homeRefresh) {
        if (homeRefresh) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Log.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.homeRefreshDone()
            } else {
                Log.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.homeRefreshDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }

    Column {
        AnimatedVisibility(
            visible = scrollState.isScrollingUp(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            HomeTopAppBar(navController)
        }
        AnimatedVisibility(
            visible = !scrollState.isScrollingUp(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.statusBars
                    )
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(chipRowState)
                .padding(vertical = 8.dp, horizontal = 15.dp),
        ) {
            Config.listOfHomeChip.forEach { id ->
                Spacer(modifier = Modifier.width(4.dp))
                Chip(
                    isSelected =
                    when (params) {
                        Constants.HOME_PARAMS_RELAX -> id == R.string.relax
                        Constants.HOME_PARAMS_SLEEP -> id == R.string.sleep
                        Constants.HOME_PARAMS_ENERGIZE -> id == R.string.energize
                        Constants.HOME_PARAMS_SAD -> id == R.string.sad
                        Constants.HOME_PARAMS_ROMANCE -> id == R.string.romance
                        Constants.HOME_PARAMS_FEEL_GOOD -> id == R.string.feel_good
                        Constants.HOME_PARAMS_WORKOUT -> id == R.string.workout
                        Constants.HOME_PARAMS_PARTY -> id == R.string.party
                        Constants.HOME_PARAMS_COMMUTE -> id == R.string.commute
                        Constants.HOME_PARAMS_FOCUS -> id == R.string.focus
                        else -> id == R.string.all
                    }, text = stringResource(id = id)
                ) {
                    when (id) {
                        R.string.all -> viewModel.setParams(null)
                        R.string.relax -> viewModel.setParams(Constants.HOME_PARAMS_RELAX)
                        R.string.sleep -> viewModel.setParams(Constants.HOME_PARAMS_SLEEP)
                        R.string.energize -> viewModel.setParams(Constants.HOME_PARAMS_ENERGIZE)
                        R.string.sad -> viewModel.setParams(Constants.HOME_PARAMS_SAD)
                        R.string.romance -> viewModel.setParams(Constants.HOME_PARAMS_ROMANCE)
                        R.string.feel_good -> viewModel.setParams(Constants.HOME_PARAMS_FEEL_GOOD)
                        R.string.workout -> viewModel.setParams(Constants.HOME_PARAMS_WORKOUT)
                        R.string.party -> viewModel.setParams(Constants.HOME_PARAMS_PARTY)
                        R.string.commute -> viewModel.setParams(Constants.HOME_PARAMS_COMMUTE)
                        R.string.focus -> viewModel.setParams(Constants.HOME_PARAMS_FOCUS)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        PullToRefreshBox(
            modifier =
            Modifier
                .padding(vertical = 8.dp),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = PullToRefreshDefaults.containerColor,
                    color = PullToRefreshDefaults.indicatorColor,
                    threshold = PullToRefreshDefaults.PositionalThreshold
                )
            }
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 15.dp),
                        state = scrollState,
                    ) {
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = accountInfo != null && accountShow,
                            ) {
                                AccountLayout(
                                    accountName = accountInfo?.first ?: "",
                                    url = accountInfo?.second ?: "",
                                )
                            }
                        }
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible =
                                homeData.find {
                                    it.title ==
                                        context.getString(
                                            R.string.quick_picks,
                                        )
                                } != null,
                            ) {
                                QuickPicks(
                                    homeItem =
                                    homeData.find {
                                        it.title ==
                                            context.getString(
                                                R.string.quick_picks,
                                            )
                                    } ?: return@AnimatedVisibility,
                                    viewModel = viewModel,
                                )
                            }
                        }
                        items(homeData, key = { it.hashCode() }) {
                            if (it.title != context.getString(R.string.quick_picks)) {
                                HomeItem(
                                    homeViewModel = viewModel,
                                    navController = navController,
                                    data = it,
                                )
                            }
                        }
                        items(newRelease, key = { it.hashCode() }) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = newRelease.isNotEmpty(),
                            ) {
                                HomeItem(
                                    homeViewModel = viewModel,
                                    navController = navController,
                                    data = it,
                                )
                            }
                        }
                        item {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = moodMomentAndGenre != null,
                            ) {
                                moodMomentAndGenre?.let {
                                    MoodMomentAndGenre(
                                        mood = it,
                                        navController = navController,
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ChartTitle()
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(targetState = regionChart) {
                                    Log.w("HomeScreen", "regionChart: $it")
                                    if (it != null) {
                                        DropdownButton(
                                            items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                            defaultSelected =
                                            CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                            )
                                                ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                        ) {
                                            viewModel.exploreChart(
                                                CHART_SUPPORTED_COUNTRY.items[
                                                    CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                        it,
                                                    ),
                                                ],
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(
                                    targetState = chartLoading,
                                    label = "Chart",
                                ) { loading ->
                                    if (!loading) {
                                        chart?.let {
                                            ChartData(
                                                chart = it,
                                                viewModel = viewModel,
                                                navController = navController,
                                                context = context,
                                            )
                                        }
                                    } else {
                                        CenterLoadingBox(
                                            modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(400.dp),
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            EndOfPage()
                        }
                    }
                } else {
                    HomeShimmer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour =
        remember {
            val date = Calendar.getInstance().time
            val formatter = SimpleDateFormat("HH")
            formatter.format(date).toInt()
        }
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = typo.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text =
                    when (hour) {
                        in 4..12 -> {
                            stringResource(R.string.good_morning)
                        }

                        in 12..16 -> {
                            stringResource(R.string.good_afternoon)
                        }

                        in 16..23 -> {
                            stringResource(R.string.good_evening)
                        }

                        else -> {
                            stringResource(R.string.good_night)
                        }
                    },
                    style = typo.bodySmall,
                )
            }
        },
        actions = {
            RippleIconButton(resId = R.drawable.outline_notifications_24) {
                navController.navigateSafe(R.id.action_global_notificationFragment)
            }
            RippleIconButton(resId = R.drawable.baseline_history_24) {
                navController.navigateSafe(
                    R.id.action_bottom_navigation_item_home_to_recentlySongsFragment,
                )
            }
            RippleIconButton(resId = R.drawable.baseline_settings_24) {
                navController.navigateSafe(
                    R.id.action_bottom_navigation_item_home_to_settingsFragment,
                )
            }
        },
    )
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        Text(
            text = stringResource(id = R.string.welcome_back),
            style = typo.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(url)
                    .crossfade(true)
                    .build(),
                placeholder = painterResource(R.drawable.holder),
                error = painterResource(R.drawable.holder),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                Modifier
                    .size(40.dp)
                    .clip(
                        CircleShape,
                    ),
            )
            Text(
                text = accountName,
                style = typo.headlineMedium,
                color = Color.White,
                modifier =
                Modifier
                    .padding(start = 8.dp),
            )
        }
    }
}

@UnstableApi
@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    viewModel: HomeViewModel
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Text(
            text = stringResource(id = R.string.let_s_start_with_a_radio),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.quick_picks),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(280.dp),
            state = lazyListState,
            flingBehavior = snapperFlingBehavior,
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null
                                )
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        data = it,
                        widthDp = widthDp,
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {

    val lazyListState1 = rememberLazyGridState()
    val snapperFlingBehavior1 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState1))

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.let_s_pick_a_playlist_for_you),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.moods_amp_moment),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(210.dp),
            state = lazyListState1,
            flingBehavior = snapperFlingBehavior1,
        ) {
            items(mood.moodsMoments, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigateSafe(
                        R.id.action_global_moodFragment,
                        Bundle().apply {
                            putString("params", it.params)
                        },
                    )
                }
            }
        }
        Text(
            text = stringResource(id = R.string.genre),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3), modifier = Modifier.height(210.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(mood.genres, key = { it.title }) {
                MoodMomentAndGenreHomeItem(title = it.title) {
                    navController.navigateSafe(
                        R.id.action_global_moodFragment,
                        Bundle().apply {
                            putString("params", it.params)
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(id = R.string.what_is_best_choice_today),
            style = typo.bodyMedium,
        )
        Text(
            text = stringResource(id = R.string.chart),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
        )
    }
}

@UnstableApi
@Composable
fun ChartData(
    chart: Chart,
    viewModel: HomeViewModel,
    navController: NavController,
    context: Context,
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState = rememberLazyListState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))

    val lazyListState1 = rememberLazyGridState()
    val snapperFlingBehavior1 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState1))

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    val lazyListState3 = rememberLazyGridState()
    val snapperFlingBehavior3 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState3))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        }
    ) {
        AnimatedVisibility(
            visible = !chart.songs.isNullOrEmpty(),
            enter = fadeIn(animationSpec = tween(2000)),
            exit = fadeOut(animationSpec = tween(2000)),
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.top_tracks),
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
                if (!chart.songs.isNullOrEmpty()) {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier.height(210.dp),
                        state = lazyListState1,
                        flingBehavior = snapperFlingBehavior1,
                    ) {
                        items(chart.songs, key = { it.videoId }) {
                            ItemTrackChart(onClick = {
                                viewModel.setQueueData(
                                    QueueData(
                                        listTracks = arrayListOf(it),
                                        firstPlayedTrack = it,
                                        playlistName = "\"${it.title}\" ${context.getString(R.string.in_charts)}",
                                        playlistType = PlaylistType.RADIO,
                                        playlistId = "RDAMVM${it.videoId}",
                                        continuation = null
                                    )
                                )
                                viewModel.loadMediaItem(
                                    data,
                                    type = Config.VIDEO_CLICK,
                                )
                            }, data = it, position = chart.songs.indexOf(it) + 1, widthDp = gridWidthDp)
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(id = R.string.top_videos),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
        LazyRow(
            state = lazyListState,
            flingBehavior = snapperFlingBehavior
        ) {
            items(chart.videos.items.size, key = { index -> chart.videos.items[index].videoId }) {
                val data = chart.videos.items[it]
                ItemVideoChart(
                    onClick = {
                        val firstQueue: Track = data.toTrack()
                        viewModel.setQueueData(
                            QueueData(
                                listTracks = arrayListOf(firstQueue),
                                firstPlayedTrack = firstQueue,
                                playlistName = "\"${data.title}\" ${context.getString(R.string.in_charts)}",
                                playlistType = PlaylistType.RADIO,
                                playlistId = "RDAMVM${data.videoId}",
                                continuation = null
                            )
                        )
                        viewModel.loadMediaItem(
                            firstQueue,
                            type = Config.VIDEO_CLICK,
                        )
                    },
                    data = data,
                    position = it + 1,
                )
            }
        }
        Text(
            text = stringResource(id = R.string.top_artists),
            style = typo.headlineMedium,
            maxLines = 1,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3), modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(onClick = {
                    val args = Bundle()
                    args.putString("channelId", data.browseId)
                    navController.navigateSafe(R.id.action_global_artistFragment, args)
                }, data = data, context = context, widthDp = gridWidthDp)
            }
        }
        AnimatedVisibility(visible = !chart.trending.isNullOrEmpty()) {
            Column {
                Text(
                    text = stringResource(id = R.string.trending),
                    style = typo.headlineMedium,
                    maxLines = 1,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
                if (!chart.trending.isNullOrEmpty()) {
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(3),
                        modifier = Modifier.height(210.dp),
                        state = lazyListState3,
                        flingBehavior = snapperFlingBehavior3,
                    ) {
                        items(chart.trending.size, key = { index ->
                            val item = chart.trending[index]
                            item.videoId
                        }) {
                            val data = chart.trending[it]
                            ItemTrackChart(onClick = {
                                viewModel.setQueueData(
                                    QueueData(
                                        listTracks = arrayListOf(data),
                                        firstPlayedTrack = data,
                                        playlistName = "\"${data.title}\" ${context.getString(R.string.in_charts)}",
                                        playlistType = PlaylistType.RADIO,
                                        playlistId = "RDAMVM${data.videoId}",
                                        continuation = null
                                    )
                                )
                                viewModel.loadMediaItem(
                                    data,
                                    type = Config.VIDEO_CLICK,
                                )
                            }, data = data, position = null, widthDp = gridWidthDp)
                        }
                    }
                }
            }
        }
    }
}