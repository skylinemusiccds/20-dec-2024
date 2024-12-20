package com.universe.audioflare.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.universe.kotlinytmusicscraper.pages.BrowseResult
import com.universe.audioflare.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MoreAlbumsViewModel(application: Application) : BaseViewModel(application) {

    override val tag: String
        get() = "MoreAlbumsViewModel"

    private var _browseResult: MutableStateFlow<BrowseResult?> = MutableStateFlow(null)
    val browseResult: StateFlow<BrowseResult?> = _browseResult

    fun getAlbumMore(id: String) {
        viewModelScope.launch {
            _browseResult.value = null
            mainRepository.getAlbumMore(id, ALBUM_PARAM).collect { data ->
                _browseResult.value = data
            }
        }
    }
    fun getSingleMore(id: String) {
        viewModelScope.launch {
            _browseResult.value = null
            mainRepository.getAlbumMore(id, SINGLE_PARAM).collect { data ->
                _browseResult.value = data
            }
        }
    }

    companion object {
        const val ALBUM_PARAM = "ggMIegYIARoCAQI%3D"
        const val SINGLE_PARAM = "ggMIegYIAhoCAQI%3D"
    }
}