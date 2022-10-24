package ru.yakaska.flexplayer.media.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/*
*
* Gran Merci for Udeon Music player!!!
*
* */

@OptIn(ExperimentalCoroutinesApi::class)
internal fun ContentResolver.observeContentChanges(contentUri: Uri): Flow<Unit> {
    require(contentUri.scheme == ContentResolver.SCHEME_CONTENT)

    return callbackFlow {
        // Trigger initial load.
        send(Unit)

        // Register an observer until flow is cancelled, and reload whenever it is notified.
        val observer = ChannelContentObserver(channel)
        registerContentObserver(contentUri, true, observer)

        awaitClose { unregisterContentObserver(observer) }
    }
}

private class ChannelContentObserver(
    private val channel: SendChannel<Unit>,
) : ContentObserver(null) {

    override fun deliverSelfNotifications(): Boolean = false

    override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange)

    override fun onChange(selfChange: Boolean) {
        channel.trySend(Unit)
    }
}

internal fun Uri.withAppendedId(id: Long) = ContentUris.withAppendedId(this, id)

