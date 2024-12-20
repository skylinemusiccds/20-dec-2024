package com.universe.audioflare.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.universe.audioflare.data.db.entities.ArtistEntity
import com.universe.audioflare.viewModel.base.BaseViewModel
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class FollowedViewModel(application: Application): BaseViewModel(application) {

    override val tag: String
        get() = "FollowedViewModel"

    private var _listFollowedArtist: MutableLiveData<ArrayList<ArtistEntity>> = MutableLiveData()
    val listFollowedArtist: LiveData<ArrayList<ArtistEntity>> get() = _listFollowedArtist

    fun getListLikedSong() {
        viewModelScope.launch {
            mainRepository.getFollowedArtists().collect{ likedSong ->
                _listFollowedArtist.value = likedSong as ArrayList<ArtistEntity>
            }
        }
    }
}