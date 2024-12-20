package com.universe.audioflare.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.universe.audioflare.data.db.entities.NotificationEntity
import com.universe.audioflare.viewModel.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class NotificationViewModel(application: Application) : BaseViewModel(application) {

    override val tag: String
        get() = "NotificationViewModel"

        private var _listNotification: MutableStateFlow<List<NotificationEntity>?> =
            MutableStateFlow(null)
        val listNotification: StateFlow<List<NotificationEntity>?> = _listNotification

        init {
            viewModelScope.launch {
                mainRepository.getAllNotifications().collect { notificationEntities ->
                    _listNotification.value = notificationEntities?.sortedByDescending {
                        it.time
                    }
                }
            }
        }
    }
