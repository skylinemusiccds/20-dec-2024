package com.universe.audioflare.viewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.universe.audioflare.R
import com.universe.audioflare.common.Config
import com.universe.audioflare.common.DownloadState
import com.universe.audioflare.data.dataStore.DataStoreManager
import com.universe.audioflare.data.db.entities.AlbumEntity
import com.universe.audioflare.data.db.entities.LocalPlaylistEntity
import com.universe.audioflare.data.db.entities.PairSongLocalPlaylist
import com.universe.audioflare.data.db.entities.PlaylistEntity
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.searchResult.playlists.PlaylistsResult
import com.universe.audioflare.data.type.PlaylistType
import com.universe.audioflare.data.type.RecentlyType
import com.universe.audioflare.utils.LocalResource
import com.universe.audioflare.viewModel.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.annotation.KoinViewModel
import java.time.LocalDateTime

@KoinViewModel
@UnstableApi
class LibraryViewModel(
        private val application: Application
    ) : BaseViewModel(application) {

    override val tag: String
        get() = "LibraryViewModel"

        private val _nowPlayingVideoId: MutableStateFlow<String> = MutableStateFlow("")
        val nowPlayingVideoId: StateFlow<String> get() = _nowPlayingVideoId

        private val _recentlyAdded: MutableStateFlow<LocalResource<List<RecentlyType>>> =
            MutableStateFlow(LocalResource.Loading())
        val recentlyAdded: StateFlow<LocalResource<List<RecentlyType>>> get() = _recentlyAdded

        private val _yourLocalPlaylist: MutableStateFlow<LocalResource<List<LocalPlaylistEntity>>> =
            MutableStateFlow(LocalResource.Loading())
        val yourLocalPlaylist: StateFlow<LocalResource<List<LocalPlaylistEntity>>> get() = _yourLocalPlaylist

        private val _youTubePlaylist: MutableStateFlow<LocalResource<List<PlaylistsResult>>> =
            MutableStateFlow(LocalResource.Loading())
        val youTubePlaylist: StateFlow<LocalResource<List<PlaylistsResult>>> get() = _youTubePlaylist

        private val _favoritePlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
            MutableStateFlow(LocalResource.Loading())
        val favoritePlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _favoritePlaylist

        private val _downloadedPlaylist: MutableStateFlow<LocalResource<List<PlaylistType>>> =
            MutableStateFlow(LocalResource.Loading())
        val downloadedPlaylist: StateFlow<LocalResource<List<PlaylistType>>> get() = _downloadedPlaylist

        private var _listRecentlyAdded: MutableLiveData<List<Any>> = MutableLiveData()
        val listRecentlyAdded: LiveData<List<Any>> = _listRecentlyAdded

        private var _listPlaylistFavorite: MutableLiveData<List<Any>> = MutableLiveData()
        val listPlaylistFavorite: LiveData<List<Any>> = _listPlaylistFavorite

        private var _listDownloadedPlaylist: MutableLiveData<List<Any>> = MutableLiveData()
        val listDownloadedPlaylist: LiveData<List<Any>> = _listDownloadedPlaylist

        private var _listLocalPlaylist: MutableLiveData<List<LocalPlaylistEntity>> = MutableLiveData()
        val listLocalPlaylist: LiveData<List<LocalPlaylistEntity>> = _listLocalPlaylist

        private var _listYouTubePlaylist: MutableLiveData<List<Any>?> = MutableLiveData()
        val listYouTubePlaylist: LiveData<List<Any>?> = _listYouTubePlaylist

        private var _songEntity: MutableLiveData<SongEntity?> = MutableLiveData()
        val songEntity: LiveData<SongEntity?> = _songEntity

        @OptIn(ExperimentalCoroutinesApi::class)
        val youtubeLoggedIn = dataStoreManager.loggedIn.mapLatest { it == DataStoreManager.TRUE }

        //    val recentlyAdded = mainRepository.getAllRecentData().map { pagingData ->
//        pagingData.map { it }
//    }.cachedIn(viewModelScope)
        init {
            getNowPlayingVideoId()
        }
        private fun getNowPlayingVideoId() {
            viewModelScope.launch {
                combine(simpleMediaServiceHandler.nowPlayingState, simpleMediaServiceHandler.controlState) { nowPlayingState, controlState ->
                    Pair(nowPlayingState, controlState)
                }.collect { (nowPlayingState, controlState) ->
                    if (controlState.isPlaying) {
                        _nowPlayingVideoId.value = nowPlayingState.songEntity?.videoId ?: ""
                    } else {
                        _nowPlayingVideoId.value = ""
                    }
                }

            }
        }

        fun getRecentlyAdded() {
            viewModelScope.launch {
                val temp: MutableList<RecentlyType> = mutableListOf()
                mainRepository.getAllRecentData().collect { data ->
                    temp.addAll(data)
                    temp.find {
                        it is PlaylistEntity && (it.id.contains("RDEM") || it.id.contains("RDAMVM"))
                    }.let {
                        temp.remove(it)
                    }
                    temp.removeIf { it is SongEntity && it.inLibrary == Config.REMOVED_SONG_DATE_TIME }
                    temp.reverse()
                    _recentlyAdded.value = LocalResource.Success(temp)
                }
            }
        }

        fun getYouTubePlaylist() {
            _youTubePlaylist.value = LocalResource.Loading()
            viewModelScope.launch {
                mainRepository.getLibraryPlaylist().collect { data ->
//                    _listYouTubePlaylist.postValue(data?.reversed())
                    _youTubePlaylist.value = LocalResource.Success(data ?: emptyList())
                }
            }
        }

        fun getYouTubeLoggedIn(): Boolean {
            return runBlocking { dataStoreManager.loggedIn.first() } == DataStoreManager.TRUE
        }

        fun getPlaylistFavorite() {
            viewModelScope.launch {
                mainRepository.getLikedAlbums().collect { album ->
                    val temp: MutableList<PlaylistType> = mutableListOf()
                    temp.addAll(album)
                    mainRepository.getLikedPlaylists().collect { playlist ->
                        temp.addAll(playlist)
                        val sortedList =
                            temp.sortedWith<PlaylistType>(
                                Comparator { p0, p1 ->
                                    val timeP0: LocalDateTime? =
                                        when (p0) {
                                            is AlbumEntity -> p0.inLibrary
                                            is PlaylistEntity -> p0.inLibrary
                                            else -> null
                                        }
                                    val timeP1: LocalDateTime? =
                                        when (p1) {
                                            is AlbumEntity -> p1.inLibrary
                                            is PlaylistEntity -> p1.inLibrary
                                            else -> null
                                        }
                                    if (timeP0 == null || timeP1 == null) {
                                        return@Comparator if (timeP0 == null && timeP1 == null) {
                                            0
                                        } else if (timeP0 == null) {
                                            -1
                                        } else {
                                            1
                                        }
                                    }
                                    timeP0.compareTo(timeP1) // Sort in descending order by inLibrary time
                                },
                            )
                        _favoritePlaylist.value = LocalResource.Success(sortedList)
                    }
                }
            }
        }

        fun getLocalPlaylist() {
            _yourLocalPlaylist.value = LocalResource.Loading()
            viewModelScope.launch {
                mainRepository.getAllLocalPlaylists().collect { values ->
//                    _listLocalPlaylist.postValue(values)
                    _yourLocalPlaylist.value = LocalResource.Success(values.reversed())
                }
            }
        }

        fun getDownloadedPlaylist() {
            viewModelScope.launch {
                mainRepository.getAllDownloadedPlaylist().collect { values ->
                    _downloadedPlaylist.value = LocalResource.Success(values)
                }
            }
        }

        fun getSongEntity(videoId: String) {
            viewModelScope.launch {
                mainRepository.getSongById(videoId).collect { values ->
                    _songEntity.value = values
                }
            }
        }

        fun updateLikeStatus(
            videoId: String,
            likeStatus: Int,
        ) {
            viewModelScope.launch {
                mainRepository.updateLikeStatus(likeStatus = likeStatus, videoId = videoId)
            }
        }

        fun createPlaylist(title: String) {
            viewModelScope.launch {
                val localPlaylistEntity = LocalPlaylistEntity(title = title)
                mainRepository.insertLocalPlaylist(localPlaylistEntity)
                getLocalPlaylist()
            }
        }

        fun updateLocalPlaylistTracks(
            list: List<String>,
            id: Long,
        ) {
            viewModelScope.launch {
                mainRepository.getSongsByListVideoId(list).collect { values ->
                    var count = 0
                    values.forEach { song ->
                        if (song.downloadState == DownloadState.STATE_DOWNLOADED) {
                            count++
                        }
                    }
                    mainRepository.updateLocalPlaylistTracks(list, id)
                    Toast.makeText(getApplication(), application.getString(R.string.added_to_playlist), Toast.LENGTH_SHORT).show()
                    if (count == values.size) {
                        mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_DOWNLOADED, id)
                    } else {
                        mainRepository.updateLocalPlaylistDownloadState(DownloadState.STATE_NOT_DOWNLOADED, id)
                    }
                }
            }
        }

        fun addToYouTubePlaylist(
            localPlaylistId: Long,
            youtubePlaylistId: String,
            videoId: String,
        ) {
            viewModelScope.launch {
                mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(localPlaylistId, LocalPlaylistEntity.YouTubeSyncState.Syncing)
                mainRepository.addYouTubePlaylistItem(youtubePlaylistId, videoId).collect { response ->
                    if (response == "STATUS_SUCCEEDED") {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylistId,
                            LocalPlaylistEntity.YouTubeSyncState.Synced,
                        )
                        Toast.makeText(
                            getApplication(),
                            application.getString(R.string.added_to_youtube_playlist),
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        mainRepository.updateLocalPlaylistYouTubePlaylistSyncState(
                            localPlaylistId,
                            LocalPlaylistEntity.YouTubeSyncState.NotSynced,
                        )
                        Toast.makeText(getApplication(), application.getString(R.string.error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        fun updateInLibrary(videoId: String) {
            viewModelScope.launch {
                mainRepository.updateSongInLibrary(LocalDateTime.now(), videoId)
            }
        }

        fun insertPairSongLocalPlaylist(pairSongLocalPlaylist: PairSongLocalPlaylist) {
            viewModelScope.launch {
                mainRepository.insertPairSongLocalPlaylist(pairSongLocalPlaylist)
            }
        }

        fun deleteSong(videoId: String) {
            viewModelScope.launch {
                mainRepository.setInLibrary(videoId, Config.REMOVED_SONG_DATE_TIME)
                delay(500)
                getRecentlyAdded()
            }
        }
    }
