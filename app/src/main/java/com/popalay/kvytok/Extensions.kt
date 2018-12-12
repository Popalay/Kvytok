package com.popalay.kvytok

import android.content.res.Resources

val Number.dp: Int
    get() = (this.toDouble() / Resources.getSystem().displayMetrics.density).toInt()

val Number.px: Int
    get() = (this.toDouble() * Resources.getSystem().displayMetrics.density).toInt()