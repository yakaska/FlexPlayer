package ru.yakaska.flexplayer.android.context

import kotlinx.coroutines.CoroutineDispatcher

class AppDispatchers(
    val Main: CoroutineDispatcher,
    val Default: CoroutineDispatcher,
    val IO: CoroutineDispatcher,
)