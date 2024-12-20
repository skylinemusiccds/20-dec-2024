package com.universe.audioflare.data.manager

import android.content.Context
import com.universe.audioflare.data.manager.base.BaseManager

class LoadMoreManager(
    private val context: Context,
) : BaseManager(context) {
    override val tag: String
        get() = this.javaClass.simpleName
}