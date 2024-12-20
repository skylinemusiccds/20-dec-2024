package com.universe.audioflare.data.model.home

import androidx.compose.runtime.Immutable
import com.universe.audioflare.data.model.explore.mood.Mood
import com.universe.audioflare.data.model.home.chart.Chart
import com.universe.audioflare.utils.Resource


@Immutable
data class HomeResponse(
    val homeItem: Resource<ArrayList<HomeItem>>,
    val exploreMood: Resource<Mood>,
    val exploreChart: Resource<Chart>
)