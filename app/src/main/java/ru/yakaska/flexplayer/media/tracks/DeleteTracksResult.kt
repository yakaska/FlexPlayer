package ru.yakaska.flexplayer.media.tracks

import android.app.PendingIntent

sealed class DeleteTracksResult {

    // Deleted
    data class Deleted(val count: Int) : DeleteTracksResult()

    // Requires reattempt
    data class RequiresPermission(val permission: String) : DeleteTracksResult()

    // Requires consent, so modern ass 11 android user should choose which to delete
    data class RequiresUserConsent(val intent: PendingIntent) : DeleteTracksResult()
}
