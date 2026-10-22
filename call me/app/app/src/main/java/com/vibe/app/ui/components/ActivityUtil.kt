package com.vibe.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** The Activity behind a Compose context (Firebase phone auth and Google sign-in need it). */
tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No Activity")
}
