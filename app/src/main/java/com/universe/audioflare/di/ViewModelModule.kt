package com.universe.audioflare.di

import androidx.media3.common.util.UnstableApi
import com.universe.audioflare.viewModel.LibraryViewModel
import com.universe.audioflare.viewModel.NowPlayingBottomSheetViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@UnstableApi
val viewModelModule = module {
    viewModel {
        NowPlayingBottomSheetViewModel(
            application = androidApplication()
        )
    }
    viewModel {
        LibraryViewModel(
            application = androidApplication()
        )
    }
}