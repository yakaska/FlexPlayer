package ru.yakaska.flexplayer.media.provider

import android.content.ContentResolver
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore


// great way to get album arts and other media properties that are not exposed by officially
object MediaStoreInternals {

    object AudioThumbnails : BaseColumns {
        val CONTENT_URI: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority(MediaStore.AUTHORITY)
            .appendPath("external")
            .appendEncodedPath("audio/albumart")
            .build()

    }
}