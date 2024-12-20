package com.universe.audioflare.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.universe.audioflare.R
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.db.entities.AlbumEntity
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PlaylistEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.model.searchResult.playlists.PlaylistsResult
import com.universe.audioflare.data.repository.MainRepository
import com.universe.audioflare.data.type.PlaylistType
import com.universe.audioflare.extension.connectArtists
import com.universe.audioflare.extension.toListName
import com.universe.audioflare.ui.theme.typo
import kotlinx.coroutines.flow.mapNotNull
import org.koin.compose.koinInject

/**
 * This is the song item in the playlist or other places.
 */
@Composable
fun SongFullWidthItems(
    track: Track? = null,
    songEntity: SongEntity? = null,
    isPlaying: Boolean,
    onMoreClickListener: ((videoId: String) -> Unit)? = null,
    onClickListener: ((videoId: String) -> Unit)? = null,
    modifier: Modifier,
) {
    val mainRepository: MainRepository = koinInject()
    val downloadState by mainRepository.getSongAsFlow(songEntity?.videoId ?: track?.videoId ?: "")
        .mapNotNull { it?.downloadState }
        .collectAsState(initial = DownloadState.STATE_NOT_DOWNLOADED)
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.audio_playing_animation),
    )
    Box(
        modifier =
            modifier.clickable{
                onClickListener?.invoke(track?.videoId ?: songEntity?.videoId ?: "")
            }.animateContentSize()
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.size(50.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        val thumb = track?.thumbnails?.lastOrNull()?.url ?: songEntity?.thumbnails
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumb)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(thumb)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.holder),
                            error = painterResource(R.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier =
                                Modifier
                                    .fillMaxSize(),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track?.title ?: songEntity?.title ?: "",
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately
                            )
                            .focusable(),
                )
                Row {
                    AnimatedVisibility(
                        visible =
                            if (songEntity != null || track != null) {
                                downloadState == DownloadState.STATE_DOWNLOADED
                            } else {
                                false
                            },
                    ) {
                        Row {
                            Icon(
                                painter = painterResource(id = R.drawable.download_for_offline_white),
                                tint = Color.White,
                                contentDescription = "",
                                modifier = Modifier.size(20.dp).padding(2.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                    Text(
                        text =
                            (
                                track?.artists?.toListName()?.connectArtists()
                                    ?: songEntity?.artistName?.connectArtists()
                            ) ?: "",
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(align = Alignment.CenterVertically)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately
                                )
                                .focusable(),
                    )
                }
            }
            RippleIconButton(resId = R.drawable.baseline_more_vert_24, fillMaxSize = false) {
                val videoId = track?.videoId ?: songEntity?.videoId
                videoId?.let { onMoreClickListener?.invoke(it) }
            }
        }
    }
}

@Composable
fun SuggestItems(
    track: Track,
    isPlaying: Boolean,
    onClickListener: (() -> Unit)? = null,
    onAddClickListener: (() -> Unit)? = null,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.audio_playing_animation),
    )
    Box(
        modifier =
            Modifier.clickable {
                if (onClickListener != null) {
                    onClickListener()
                }
            }.animateContentSize()
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp)
                .fillMaxWidth(),
        ) {
            Box(modifier = Modifier.size(40.dp)) {
                Crossfade(isPlaying) {
                    if (it) {
                        LottieAnimation(composition, iterations = LottieConstants.IterateForever)
                    } else {
                        val thumb = track.thumbnails?.lastOrNull()?.url
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumb)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .diskCacheKey(thumb)
                                .crossfade(true)
                                .build(),
                            placeholder = painterResource(R.drawable.holder),
                            error = painterResource(R.drawable.holder),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier =
                                Modifier
                                    .wrapContentHeight()
                                    .fillMaxWidth(),
                        )
                    }
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = track.title,
                    style = typo.titleSmall,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately
                            )
                            .focusable(),
                )
                Text(
                    text =
                        (
                            track.artists?.toListName()?.connectArtists()
                        ) ?: "",
                    style = typo.bodySmall,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately
                            )
                            .focusable(),
                )
            }
            RippleIconButton(
                resId = R.drawable.baseline_add_24,
                fillMaxSize = false,
                onClick =
                    onAddClickListener ?: {
                    },
            )
        }
    }
}

@Composable
fun PlaylistFullWidthItems(
    data: PlaylistType,
    onClickListener: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
        modifier.clickable{
            onClickListener?.invoke()
        }.animateContentSize()
    ) {
        var title = ""
        var thumb = ""
        var firstSubtitle = ""
        var secondSubtitle = ""
        var thirdRowSubtitle: String? = null

        when (data.playlistType()) {
            PlaylistType.Type.YOUTUBE_PLAYLIST -> firstSubtitle = stringResource(id = R.string.playlist)
            PlaylistType.Type.RADIO -> firstSubtitle = stringResource(id = R.string.radio)
            PlaylistType.Type.LOCAL -> firstSubtitle = stringResource(id = R.string.playlist)
            PlaylistType.Type.ALBUM -> firstSubtitle = stringResource(id = R.string.album)
        }
        when (data) {
            is AlbumEntity -> {
                title = data.title
                thumb = data.thumbnails ?: ""
                secondSubtitle = data.artistName?.connectArtists() ?: ""
                thirdRowSubtitle = data.year
            }
            is PlaylistEntity -> {
                title = data.title
                thumb = data.thumbnails
                secondSubtitle = data.author ?: ""
            }
            is LocalPlaylistEntity -> {
                title = data.title
                thumb = data.thumbnail ?: ""
                secondSubtitle = stringResource(R.string.you)
            }
            is PlaylistsResult -> {
                title = data.title
                thumb = data.thumbnails.lastOrNull()?.url ?: ""
                secondSubtitle = data.author
            }
        }
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.size(50.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumb)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(thumb)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier =
                    Modifier
                        .fillMaxSize(),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = title,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately
                        )
                        .focusable(),
                )

                Text(
                    text = "$firstSubtitle ${if (secondSubtitle.isNotEmpty()) " • $secondSubtitle" else ""}",
                    style = typo.bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately
                        )
                        .focusable(),
                )

                if (thirdRowSubtitle != null) {
                    Text(
                        text = "$thirdRowSubtitle",
                        style = typo.bodyMedium,
                        maxLines = 1,
                        color = Color(0xC4FFFFFF),
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.CenterVertically)
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                animationMode = MarqueeAnimationMode.Immediately
                            )
                            .focusable(),
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistFullWidthItems(
    data: ArtistEntity,
    onClickListener: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .clickable {
                onClickListener?.invoke()
            }
    ) {
        Row(
            Modifier
                .padding(vertical = 10.dp, horizontal = 15.dp)
                .fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.size(50.dp)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data.thumbnails)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(data.thumbnails)
                        .crossfade(true)
                        .build(),
                    placeholder = painterResource(R.drawable.holder),
                    error = painterResource(R.drawable.holder),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 20.dp, end = 10.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Text(
                    text = data.name,
                    style = typo.labelMedium,
                    maxLines = 1,
                    color = Color.White,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately
                        )
                        .focusable(),
                )

                Text(
                    text = stringResource(R.string.artists),
                    style = typo.bodyMedium,
                    maxLines = 1,
                    color = Color(0xC4FFFFFF),
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(align = Alignment.CenterVertically)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately
                        )
                        .focusable(),
                )
            }
        }
    }
}