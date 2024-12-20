package com.universe.audioflare.ui.screen.library

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.toBitmap
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants.IterateForever
import com.airbnb.lottie.compose.rememberLottieComposition
import com.kmpalette.rememberPaletteState
import com.universe.audioflare.R
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.extension.angledGradientBackground
import com.universe.audioflare.extension.getColorFromPalette
import com.universe.audioflare.ui.component.CenterLoadingBox
import com.universe.audioflare.ui.component.EndOfPage
import com.universe.audioflare.ui.component.LoadingDialog
import com.universe.audioflare.ui.component.LocalPlaylistBottomSheet
import com.universe.audioflare.ui.component.NowPlayingBottomSheet
import com.universe.audioflare.ui.component.RippleIconButton
import com.universe.audioflare.ui.component.SongFullWidthItems
import com.universe.audioflare.ui.component.SuggestItems
import com.universe.audioflare.ui.theme.md_theme_dark_background
import com.universe.audioflare.ui.theme.typo
import com.universe.audioflare.viewModel.FilterState
import com.universe.audioflare.viewModel.LocalPlaylistUIEvent
import com.universe.audioflare.viewModel.LocalPlaylistViewModel
import com.universe.audioflare.viewModel.SharedViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import java.time.format.DateTimeFormatter

@UnstableApi
@ExperimentalFoundationApi
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalCoroutinesApi::class,
)
@Composable
fun PlaylistScreen(
    id: Long,
    sharedViewModel: SharedViewModel,
    viewModel: LocalPlaylistViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.downloading_animation),
    )

    val uiState by viewModel.uiState.collectAsState()

    val aiPainter = painterResource(id = R.drawable.baseline_tips_and_updates_24)
    val limit = 1.5f
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progressAnimated by transition.animateFloat(
        initialValue = -limit,
        targetValue = limit,
        animationSpec =
        infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
        infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    val lazyState = rememberLazyListState()
    val firstItemVisible by remember {
        derivedStateOf {
            lazyState.firstVisibleItemIndex == 0
        }
    }
    val downloadState by viewModel.uiState.map { it.downloadState }.collectAsState(
        initial = DownloadState.STATE_NOT_DOWNLOADED
    )
    var shouldHideTopBar by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestions by rememberSaveable { mutableStateOf(false) }
    var shouldShowSuggestButton by rememberSaveable { mutableStateOf(false) }

    val playingTrack by sharedViewModel.nowPlayingState
        .mapLatest {
            it?.songEntity
        }.collectAsState(initial = null)
    val isPlaying by sharedViewModel.controllerState.map { it.isPlaying }.collectAsState(initial = false)
    val suggestedTracks by viewModel.uiState.map { it.suggestions?.songs ?: emptyList() }.collectAsState(initial = emptyList())
    val suggestionsLoading by viewModel.loading.collectAsState()
    var showSyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsyncAlertDialog by rememberSaveable { mutableStateOf(false) }
    var firstTimeGetLocalPlaylist by rememberSaveable {
        mutableStateOf(false)
    }

    var currentItem by remember {
        mutableStateOf<SongEntity?>(null)
    }

    var itemBottomSheetShow by remember {
        mutableStateOf(false)
    }
    var playlistBottomSheetShow by remember {
        mutableStateOf(false)
    }

    val trackPagingItems: LazyPagingItems<SongEntity> = viewModel.tracksPagingState.collectAsLazyPagingItems()
    LaunchedEffect(Unit) {
        snapshotFlow {
            trackPagingItems.loadState
        }.collectLatest {
            Log.d("PlaylistScreen", "loadState: ${trackPagingItems.loadState}")
            viewModel.setLazyTrackPagingItems(trackPagingItems)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            firstTimeGetLocalPlaylist = true
        }
    }

    val onPlaylistItemClick: (videoId: String) -> Unit = { videoId ->
        viewModel.onUIEvent(
            LocalPlaylistUIEvent.ItemClick(
                videoId = videoId
            )
        )
    }
    val onItemMoreClick: (videoId: String) -> Unit = { videoId ->
        currentItem = trackPagingItems.itemSnapshotList.findLast { it?.videoId == videoId }
        if (currentItem != null) {
            itemBottomSheetShow = true
        }
    }
    val onPlaylistMoreClick: () -> Unit = {
        playlistBottomSheetShow = true
    }

    LaunchedEffect(key1 = shouldShowSuggestions) {
        if (suggestedTracks.isEmpty() && uiState.syncState != LocalPlaylistEntity.YouTubeSyncState.NotSynced) {
            viewModel.getSuggestions(uiState.id)
        }
    }

    LaunchedEffect(key1 = id) {
        if (id != uiState.id) {
            Log.w("PlaylistScreen", "new id: $id")
            viewModel.setOffset(0)
            viewModel.removeListSuggestion()
            viewModel.updatePlaylistState(id, true)
            delay(100)
            firstTimeGetLocalPlaylist = true
        }
    }
    LaunchedEffect(key1 = uiState) {
        shouldShowSuggestButton =
            !uiState.ytPlaylistId.isNullOrEmpty() && uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced
    }
    LaunchedEffect(key1 = firstItemVisible) {
        shouldHideTopBar = !firstItemVisible
    }
    val paletteState = rememberPaletteState()
    var bitmap by remember {
        mutableStateOf<ImageBitmap?>(null)
    }

    LaunchedEffect(bitmap) {
        val bm = bitmap
        if (bm != null) {
            paletteState.generate(bm)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { paletteState.palette }
            .distinctUntilChanged()
            .collectLatest {
                viewModel.setBrush(listOf(it.getColorFromPalette(), md_theme_dark_background))
            }
    }

    // Loading dialog
    val showLoadingDialog by viewModel.showLoadingDialog.collectAsState()
    if (showLoadingDialog.first) {
        LoadingDialog(
            true,
            showLoadingDialog.second,
        )
    }
//    Box {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(Color.Black),
        state = lazyState,
    ) {
        item(contentType = "header") {
            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.Transparent),
            ) {
                Box(
                    modifier =
                    Modifier
                        .fillMaxWidth(),
//                                .haze(
//                                    hazeState,
//                                    style = HazeMaterials.regular(),
//                                ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(
                                    RoundedCornerShape(8.dp),
                                ).angledGradientBackground(uiState.colors, 25f),
                    )
                    Box(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                brush =
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0x75000000),
                                        Color.Black,
                                    ),
                                ),
                            ),
                    )
                }
                Column(
                    Modifier
                        .background(Color.Transparent),
//                            .hazeChild(hazeState, style = HazeMaterials.regular()),
                ) {
                    Row(
                        modifier =
                        Modifier
                            .wrapContentWidth()
                            .padding(16.dp)
                            .windowInsetsPadding(WindowInsets.statusBars),
                    ) {
                        RippleIconButton(
                            resId = R.drawable.baseline_arrow_back_ios_new_24,
                        ) {
                            navController.popBackStack()
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.Start,
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(uiState.thumbnail)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(uiState.thumbnail)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.holder),
                            error = painterResource(R.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillHeight,
                            onSuccess = {
                                bitmap = it.result.image.toBitmap().asImageBitmap()
                            },
                            modifier =
                            Modifier
                                .height(250.dp)
                                .wrapContentWidth()
                                .align(Alignment.CenterHorizontally)
                                .clip(
                                    RoundedCornerShape(8.dp),
                                ),
                        )
                        Box(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                        ) {
                            Column(Modifier.padding(horizontal = 32.dp)) {
                                Spacer(modifier = Modifier.size(25.dp))
                                Text(
                                    text = uiState.title,
                                    style = typo.titleLarge,
                                    color = Color.White,
                                )
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.your_playlist),
                                        style = typo.titleSmall,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text =
                                            stringResource(
                                                id = R.string.created_at,
                                                uiState.inLibrary?.format(
                                                    DateTimeFormatter.ofPattern(
                                                        "kk:mm - dd MMM uuuu",
                                                    ),
                                                ) ?: "",
                                            ),
                                        style = typo.bodyMedium,
                                        color = Color(0xC4FFFFFF),
                                    )
                                }
                                Row(
                                    modifier =
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RippleIconButton(
                                        resId = R.drawable.baseline_play_circle_24,
                                        fillMaxSize = true,
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        viewModel.onUIEvent(LocalPlaylistUIEvent.PlayClick)
                                    }
                                    Spacer(modifier = Modifier.size(5.dp))
                                    Crossfade(targetState = downloadState) {
                                        when (it) {
                                            DownloadState.STATE_DOWNLOADED -> {
                                                Box(
                                                    modifier =
                                                    Modifier
                                                        .size(36.dp)
                                                        .clip(
                                                            CircleShape,
                                                        )
                                                        .clickable {
                                                            Toast
                                                                .makeText(
                                                                    context,
                                                                    context.getString(R.string.downloaded),
                                                                    Toast.LENGTH_SHORT,
                                                                )
                                                                .show()
                                                        },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.baseline_downloaded),
                                                        tint = Color(0xFF00A0CB),
                                                        contentDescription = "",
                                                        modifier =
                                                        Modifier
                                                            .size(36.dp)
                                                            .padding(2.dp),
                                                    )
                                                }
                                            }

                                            DownloadState.STATE_DOWNLOADING -> {
                                                Box(
                                                    modifier =
                                                    Modifier
                                                        .size(36.dp)
                                                        .clip(
                                                            CircleShape,
                                                        )
                                                        .clickable {
                                                            Toast
                                                                .makeText(
                                                                    context,
                                                                    context.getString(R.string.downloading),
                                                                    Toast.LENGTH_SHORT,
                                                                )
                                                                .show()
                                                        },
                                                ) {
                                                    LottieAnimation(
                                                        composition,
                                                        iterations = IterateForever,
                                                        modifier = Modifier.fillMaxSize(),
                                                    )
                                                }
                                            }

                                            else -> {
                                                RippleIconButton(
                                                    fillMaxSize = true,
                                                    resId = R.drawable.download_button,
                                                    modifier = Modifier.size(36.dp),
                                                ) {
                                                    Log.w("PlaylistScreen", "downloadState: $downloadState")
                                                    viewModel.downloadFullPlaylist()
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.weight(1f))
                                    Spacer(Modifier.size(5.dp))
                                    AnimatedVisibility(visible = shouldShowSuggestButton) {
                                        Box(
                                            modifier =
                                            Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .graphicsLayer {
                                                    compositingStrategy =
                                                        CompositingStrategy.Offscreen
                                                }
                                                .clickable {
                                                    shouldShowSuggestions = !shouldShowSuggestions
                                                }
                                                .drawWithCache {
                                                    val width = size.width - 10
                                                    val height = size.height - 10

                                                    val offsetDraw = width * progressAnimated
                                                    val gradientColors =
                                                        listOf(
                                                            Color(0xFF4C82EF),
                                                            Color(0xFFD96570),
                                                        )
                                                    val brush =
                                                        Brush.linearGradient(
                                                            colors = gradientColors,
                                                            start = Offset(offsetDraw, 0f),
                                                            end =
                                                            Offset(
                                                                offsetDraw + width,
                                                                height,
                                                            ),
                                                        )

                                                    onDrawBehind {
                                                        // Destination
                                                        with(aiPainter) {
                                                            draw(
                                                                size = Size(width, width),
                                                            )
                                                        }

                                                        // Source
                                                        drawRect(
                                                            brush = brush,
                                                            blendMode = BlendMode.SrcIn,
                                                        )
                                                    }
                                                },
                                        )
                                    }
                                    RippleIconButton(
                                        modifier =
                                        Modifier.size(36.dp),
                                        resId = R.drawable.baseline_shuffle_24,
                                        fillMaxSize = true,
                                    ) {
                                        viewModel.onUIEvent(LocalPlaylistUIEvent.ShuffleClick)
                                    }
                                    Spacer(Modifier.size(5.dp))
                                    RippleIconButton(
                                        modifier =
                                        Modifier.size(36.dp),
                                        resId = R.drawable.baseline_more_vert_24,
                                        fillMaxSize = true,
                                    ) {
                                        onPlaylistMoreClick()
                                    }
                                }
                                // Hide in local playlist
                                //                                ExpandableText(
                                //                                    modifier = Modifier.padding(vertical = 8.dp),
                                //                                    text = stringResource(id = R.string.demo_description),
                                //                                    fontSize = typo.bodyLarge.fontSize,
                                //                                    showMoreStyle = SpanStyle(Color.Gray),
                                //                                    showLessStyle = SpanStyle(Color.Gray),
                                //                                    style = TextStyle(
                                //                                        color = Color(0xC4FFFFFF)
                                //                                    )
                                //                                )
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.album_length,
                                            (uiState.trackCount).toString(),
                                            "",
                                        ),
                                    color = Color.White,
                                    style = typo.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                                AnimatedVisibility(visible = shouldShowSuggestions) {
                                    Column(
                                        modifier = Modifier.animateContentSize(),
                                    ) {
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            text =
                                            stringResource(
                                                id = R.string.suggest,
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 8.dp),
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Crossfade(targetState = suggestionsLoading) {
                                            if (it) {
                                                CenterLoadingBox(
                                                    modifier =
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .align(Alignment.CenterHorizontally),
                                                )
                                            } else {
                                                Column {
                                                    suggestedTracks.forEachIndexed { index, track ->
                                                        SuggestItems(
                                                            track = track,
                                                            isPlaying = playingTrack?.videoId == track.videoId,
                                                            onAddClickListener = {
                                                                viewModel.addSuggestTrackToListTrack(
                                                                    track,
                                                                )
                                                            },
                                                            onClickListener = {
                                                                viewModel.onUIEvent(LocalPlaylistUIEvent.SuggestionsItemClick(track.videoId))
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.size(8.dp))
                                        TextButton(
                                            onClick = { viewModel.reloadSuggestion() },
                                            modifier =
                                            Modifier
                                                .padding(horizontal = 8.dp)
                                                .drawWithContent {
                                                    val strokeWidthPx = 2.dp.toPx()
                                                    val width = size.width
                                                    val height = size.height

                                                    drawContent()

                                                    with(drawContext.canvas.nativeCanvas) {
                                                        val checkPoint = saveLayer(null, null)

                                                        // Destination
                                                        drawRoundRect(
                                                            cornerRadius = CornerRadius(x = 60f, y = 60f),
                                                            color = Color.Gray,
                                                            topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2),
                                                            size = Size(width - strokeWidthPx, height - strokeWidthPx),
                                                            style = Stroke(strokeWidthPx),
                                                        )
                                                        val gradientColors =
                                                            listOf(
                                                                Color(0xFF4C82EF),
                                                                Color(0xFFD96570),
                                                            )
                                                        val brush =
                                                            Brush.linearGradient(
                                                                colors = gradientColors,
                                                                start = Offset(2f, 0f),
                                                                end =
                                                                Offset(
                                                                    2 + width,
                                                                    height,
                                                                ),
                                                            )

                                                        // Source
                                                        rotate(degrees = angle) {
                                                            drawCircle(
                                                                brush = brush,
                                                                radius = size.width,
                                                                blendMode = BlendMode.SrcIn,
                                                            )
                                                        }

                                                        restoreToCount(checkPoint)
                                                    }
                                                },
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.reload),
                                                color = Color.White,
                                                modifier =
                                                Modifier.align(
                                                    Alignment.CenterVertically,
                                                ),
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(12.dp))
                                        HorizontalDivider(
                                            color = Color.Gray,
                                            thickness = 0.5.dp,
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                                ElevatedButton(
                                    contentPadding = PaddingValues(0.dp),
                                    modifier =
                                    Modifier
                                        .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                                    onClick = {
                                        viewModel.onUIEvent(LocalPlaylistUIEvent.ChangeFilter)
                                    },
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (uiState.filterState == FilterState.OlderFirst) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.baseline_arrow_drop_down_24),
                                                contentDescription = "Older First",
                                                tint = Color.White,
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(R.drawable.baseline_arrow_drop_up_24),
                                                contentDescription = "Newer First",
                                                tint = Color.White,
                                            )
                                        }
                                        Spacer(modifier = Modifier.size(3.dp))
                                        Text(text = stringResource(id = R.string.added_date), style = typo.bodySmall, color = Color.White)
                                        Spacer(modifier = Modifier.size(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        items(count = trackPagingItems.itemCount, key = { index ->
            val item = trackPagingItems[index]
            item?.videoId ?: "item_$index"
        }) { index ->
            val item = trackPagingItems[index]
            if (item != null) {
                if (playingTrack?.videoId == item.videoId && isPlaying) {
                    SongFullWidthItems(
                        isPlaying = true,
                        songEntity = item,
                        onMoreClickListener = { onItemMoreClick(it) },
                        onClickListener = {
                            Log.w("PlaylistScreen", "index: $index")
                            onPlaylistItemClick(it)
                        },
                        modifier = Modifier.animateItem(),
                    )
                } else {
                    SongFullWidthItems(
                        isPlaying = false,
                        songEntity = item,
                        onMoreClickListener = { onItemMoreClick(it) },
                        onClickListener = {
                            Log.w("PlaylistScreen", "index: $index")
                            onPlaylistItemClick(it)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        trackPagingItems.apply {
            when {
                loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(15.dp))
                            CenterLoadingBox(modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(15.dp))
                        }
                    }
                }
            }
        }
        item {
            EndOfPage()
        }
    }
    if (itemBottomSheetShow && currentItem != null) {
        val track = currentItem ?: return
        NowPlayingBottomSheet(
            onDelete = { viewModel.deleteItem(uiState.id, track) },
            onDismiss = {
                itemBottomSheetShow = false
                currentItem = null
            },
            navController = navController,
            song = track,
        )
    }
    if (playlistBottomSheetShow) {
        Log.w("PlaylistScreen", "PlaylistBottomSheet")
        LocalPlaylistBottomSheet(
            isBottomSheetVisible = playlistBottomSheetShow,
            onDismiss = { playlistBottomSheetShow = false },
            title = uiState.title,
            ytPlaylistId = uiState.ytPlaylistId,
            onEditTitle =
                { newTitle ->
                    viewModel.updatePlaylistTitle(newTitle, uiState.id)
                },
            onEditThumbnail =
                { thumbUri ->
                    viewModel.updatePlaylistThumbnail(thumbUri, uiState.id)
                },
            onAddToQueue = {
                viewModel.addAllToQueue()
            },
            onSync = {
                if (uiState.syncState == LocalPlaylistEntity.YouTubeSyncState.Synced) {
                    showUnsyncAlertDialog = true
                } else {
                    showSyncAlertDialog = true
                }
            },
            onUpdatePlaylist = {
                viewModel.updateListTrackSynced(uiState.id)
            },
            onDelete = {
                viewModel.deletePlaylist(uiState.id)
                navController.popBackStack()
            },
        )
    }
    if (showSyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.warning)) },
            text = { Text(text = stringResource(id = R.string.sync_playlist_warning)) },
            onDismissRequest = { showSyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                        viewModel.syncPlaylistWithYouTubePlaylist(uiState.id)
                        showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
    if (showUnsyncAlertDialog) {
        AlertDialog(
            title = { Text(text = stringResource(id = R.string.warning)) },
            text = { Text(text = stringResource(id = R.string.unsync_playlist_warning)) },
            onDismissRequest = { showUnsyncAlertDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.unsyncPlaylistWithYouTubePlaylist(uiState.id)
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsyncAlertDialog = false
                }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
    AnimatedVisibility(
        visible = shouldHideTopBar,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = uiState.title,
                    style = typo.titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        R.drawable.baseline_arrow_back_ios_new_24,
                        Modifier
                            .size(32.dp),
                        true,
                    ) {
                        navController.popBackStack()
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            modifier = Modifier.angledGradientBackground(uiState.colors, 90f),
        )
    }
}