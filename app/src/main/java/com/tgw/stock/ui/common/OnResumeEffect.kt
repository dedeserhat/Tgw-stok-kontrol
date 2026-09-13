package com.tgw.stock.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Runs [onResume] every time this destination's own Lifecycle reaches RESUMED - both on
 * first entry and whenever it becomes visible again. Navigation Compose gives every
 * back stack entry its own Lifecycle, so this fires correctly for the bottom-nav tabs
 * (Dashboard/Stock/Purchase/Recipes), which use `saveState`/`restoreState` and are
 * therefore never disposed+recreated on a tab switch - a plain `LaunchedEffect(Unit)`
 * would only run once and go stale after data changes on another screen.
 */
@Composable
fun OnResumeEffect(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
