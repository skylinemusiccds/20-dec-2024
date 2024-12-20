package com.universe.audioflare.viewModel.base

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.universe.kotlinytmusicscraper.models.SongItem
import com.universe.audioflare.R
import com.universe.audioflare.common.Config.ALBUM_CLICK
import com.universe.audioflare.common.Config.PLAYLIST_CLICK
import com.universe.audioflare.common.Config.RECOVER_TRACK_QUEUE
import com.universe.audioflare.common.Config.SHARE
import com.universe.audioflare.common.Config.SONG_CLICK
import com.universe.audioflare.common.Config.VIDEO_CLICK
import com.universe.audioflare.data.dataStore.DataStoreManager
import com.universe.audioflare.data.db.entities.SongEntity
import com.universe.audioflare.data.model.browse.album.Track
import com.universe.audioflare.data.repository.MainRepository
import com.universe.audioflare.extension.toMediaItem
import com.universe.audioflare.extension.toSongEntity
import com.universe.audioflare.extension.toTrack
import com.universe.audioflare.service.QueueData
import com.universe.audioflare.service.SimpleMediaServiceHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@UnstableApi
abstract class BaseViewModel(
    private val application: Application,
) : AndroidViewModel(application),
    KoinComponent {
    protected val dataStoreManager: DataStoreManager by inject()
    protected val mainRepository: MainRepository by inject()

    // I want I can play track from any viewModel instead of calling from UI to sharedViewModel
    protected val simpleMediaServiceHandler: SimpleMediaServiceHandler by inject()

    /**
     * Tag for logging
     */
    abstract val tag: String

    /**
     * Log with viewModel tag
     */
    protected fun log(message: String, logType: Int) {
        when (logType) {
            Log.ASSERT -> Log.wtf(tag, message)
            Log.VERBOSE -> Log.v(tag, message)
            Log.DEBUG -> Log.d(tag, message)
            Log.INFO -> Log.i(tag, message)
            Log.WARN -> Log.w(tag, message)
            Log.ERROR -> Log.e(tag, message)
            else -> Log.d(tag, message)
        }
    }

    /**
     * Cancel all jobs
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
    }

    protected fun makeToast(message: String?) {
        Toast.makeText(application, message ?: "NO MESSAGE", Toast.LENGTH_SHORT).show()
    }

    protected fun getString(resId: Int): String = application.getString(resId)

    // Loading dialog
    private val _showLoadingDialog: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(false to getString(R.string.loading))
    val showLoadingDialog: StateFlow<Pair<Boolean, String>> get() = _showLoadingDialog

    fun showLoadingDialog(message: String? = null) {
        _showLoadingDialog.value = true to (message ?: getString(R.string.loading))
    }
    fun hideLoadingDialog() {
        _showLoadingDialog.value = false to getString(R.string.loading)
    }

    /**
     * Communicate with SimpleMediaServiceHandler to load media item
     */
    fun setQueueData(queueData: QueueData) {
        simpleMediaServiceHandler.setQueueData(queueData)
    }

    fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null
    ) {
        val track = when (anyTrack) {
            is Track -> anyTrack
            is SongItem -> anyTrack.toTrack()
            is SongEntity -> anyTrack.toTrack()
            else -> return
        }
        viewModelScope.launch {
            mainRepository.insertSong(track.toSongEntity()).collect {
                log("Inserted song: ${track.title}", Log.DEBUG)
            }
            simpleMediaServiceHandler.clearMediaItems()
            track.durationSeconds?.let {
                mainRepository.updateDurationSeconds(it, track.videoId)
            }
            withContext(Dispatchers.Main) {
                simpleMediaServiceHandler.addMediaItem(track.toMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)
            }
            when (type) {
                SONG_CLICK, VIDEO_CLICK, SHARE -> {
                    simpleMediaServiceHandler.getRelated(track.videoId)
                }
                PLAYLIST_CLICK, ALBUM_CLICK -> {
                    simpleMediaServiceHandler.loadPlaylistOrAlbum(index)
                }
            }
        }
    }

    fun shufflePlaylist(firstPlayIndex: Int = 0) {
        simpleMediaServiceHandler.shufflePlaylist(firstPlayIndex)
    }

    fun getQueueData() = simpleMediaServiceHandler.queueData.value
}