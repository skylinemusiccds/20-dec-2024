package com.universe.audioflare.viewModel

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.universe.audioflare.pagination.RecentPagingSource
import com.universe.audioflare.viewModel.base.BaseViewModel
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class RecentlySongsViewModel(
    application: Application,
) : BaseViewModel(application) {
    override val tag: String = "RecentlySongsViewModel"

    val recentlySongs =
        Pager(
            PagingConfig(
                pageSize = 20,
                enablePlaceholders = false,
                initialLoadSize = 20,
            ),
        ) {
            RecentPagingSource(mainRepository)
        }.flow.cachedIn(viewModelScope)
}