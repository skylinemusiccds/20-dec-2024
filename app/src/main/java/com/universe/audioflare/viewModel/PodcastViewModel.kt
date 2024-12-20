package com.universe.audioflare.viewModel

import android.app.Application
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.universe.audioflare.data.model.podcast.PodcastBrowse
import com.universe.audioflare.utils.Resource
import com.universe.audioflare.viewModel.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class PodcastViewModel(
    application: Application,
) : BaseViewModel(application) {
    override val tag: String
        get() = "PodcastViewModel"

    var gradientDrawable: MutableLiveData<GradientDrawable?> = MutableLiveData()
    var loading = MutableLiveData<Boolean>()
    var id = MutableLiveData<String>()

    private val _podcastBrowse: MutableLiveData<Resource<PodcastBrowse>?> = MutableLiveData()
    val podcastBrowse: MutableLiveData<Resource<PodcastBrowse>?> = _podcastBrowse

    fun clearPodcastBrowse() {
        _podcastBrowse.value = null
        gradientDrawable.value = null
    }

    fun getPodcastBrowse(id: String) {
        loading.value = true
        viewModelScope.launch {
            mainRepository.getPodcastData(id).collect {
                _podcastBrowse.value = it
                withContext(Dispatchers.Main) {
                    loading.value = false
                }
            }
        }
    }
}