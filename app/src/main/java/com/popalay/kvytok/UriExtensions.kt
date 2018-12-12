package com.popalay.kvytok

import android.content.Context
import android.net.Uri

fun Uri.getRealPath(context: Context): String? = FileUtils.getPath(context, this)