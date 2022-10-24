package ru.yakaska.flexplayer.media.tracks.local

import android.Manifest
import android.content.ContentResolver
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import ru.yakaska.flexplayer.android.context.AppDispatchers
import ru.yakaska.flexplayer.android.os.FileSystem
import ru.yakaska.flexplayer.android.permissions.PermissionRepository
import ru.yakaska.flexplayer.media.provider.MediaStoreInternals
import ru.yakaska.flexplayer.media.provider.observeContentChanges
import ru.yakaska.flexplayer.media.provider.withAppendedId
import ru.yakaska.flexplayer.media.tracks.DeleteTracksResult

class TrackLocalSource constructor(
    private val resolver: ContentResolver,
    private val fileSystem: FileSystem,
    private val permissionRepository: PermissionRepository,
    private val dispatchers: AppDispatchers,
) {

    val tracks: Flow<List<LocalTrack>>
        get() = combine(
            resolver.observeContentChanges(Media.EXTERNAL_CONTENT_URI),
            permissionRepository.permissions
        ) { _, permissions ->
            if (permissions.canReadAudioFiles) fetchTracks() else emptyList()
        }


    private suspend fun fetchTracks(): List<LocalTrack> = withContext(dispatchers.IO) {
        val trackColumns = arrayOf(
            BaseColumns._ID,
            Media.TITLE,
            Media.ALBUM,
            Media.ARTIST,
            Media.DURATION,
            Media.TRACK,
            Media.ALBUM_ID,
            Media.ARTIST_ID,
            Media.DATE_ADDED,
            Media.SIZE
        )

        return@withContext resolver.query(
            Media.EXTERNAL_CONTENT_URI,
            trackColumns,
            "${Media.IS_MUSIC} = 1",
            null,
            Media.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            // Memorize cursor column indexes for faster lookup
            val colId = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val colTitle = cursor.getColumnIndexOrThrow(Media.TITLE)
            val colAlbum = cursor.getColumnIndexOrThrow(Media.ALBUM)
            val colArtist = cursor.getColumnIndexOrThrow(Media.ARTIST)
            val colDuration = cursor.getColumnIndexOrThrow(Media.DURATION)
            val colTrackNo = cursor.getColumnIndexOrThrow(Media.TRACK)
            val colAlbumId = cursor.getColumnIndexOrThrow(Media.ALBUM_ID)
            val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
            val colDateAdded = cursor.getColumnIndexOrThrow(Media.DATE_ADDED)
            val colFileSize = cursor.getColumnIndexOrThrow(Media.SIZE)

            buildList(capacity = cursor.count) {
                while (cursor.moveToNext()) {
                    val trackId = cursor.getLong(colId)
                    val albumId = cursor.getLong(colAlbumId)
                    val trackNo = cursor.getInt(colTrackNo)
                    val trackUri = Media.EXTERNAL_CONTENT_URI.withAppendedId(trackId).toString()

                    this += LocalTrack(
                        id = trackId,
                        title = cursor.getString(colTitle),
                        artist = cursor.getString(colArtist),
                        album = cursor.getString(colAlbum),
                        duration = cursor.getLong(colDuration),
                        discNumber = trackNo / 1000,
                        trackNumber = trackNo % 1000,
                        mediaUri = trackUri,
                        albumArtUri = MediaStoreInternals.AudioThumbnails.CONTENT_URI
                            .withAppendedId(albumId)
                            .toString(),
                        availabilityDate = cursor.getLong(colDateAdded),
                        artistId = cursor.getLong(colArtistId),
                        albumId = albumId,
                        fileSize = cursor.getLong(colFileSize)
                    )
                }
            }
        } ?: emptyList()
    }

    suspend fun deleteTracks(trackIds: LongArray): DeleteTracksResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteTracksWithScopedStorage(trackIds)
        } else {
            deleteTracksWithoutScopedStorage(trackIds)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteTracksWithScopedStorage(ids: LongArray): DeleteTracksResult {
        val intent = MediaStore.createDeleteRequest(
            resolver,
            ids.map { trackId -> Media.EXTERNAL_CONTENT_URI.withAppendedId(trackId) }
        )
        return DeleteTracksResult.RequiresUserConsent(intent)
    }

    private suspend fun deleteTracksWithoutScopedStorage(ids: LongArray): DeleteTracksResult {
        if (!permissionRepository.permissions.value.canWriteAudioFiles) {
            return DeleteTracksResult.RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val deleteCount = withContext(dispatchers.IO) {
            var whereClause = buildInTrackIdClause(ids.size)
            var whereArgs = Array(ids.size) { ids[it].toString() }

            resolver.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(Media._ID, Media.DATA),
                whereClause,
                whereArgs,
                Media._ID
            )?.use { cursor ->
                val colId = cursor.getColumnIndexOrThrow(Media._ID)

                val colFilepath = cursor.getColumnIndexOrThrow(Media.DATA)

                val deletedTrackIds = mutableListOf<Long>()
                while (cursor.moveToNext()) {
                    val trackId = cursor.getLong(colId)
                    val path = cursor.getString(colFilepath)

                    // Attempt to delete each file.
                    if (fileSystem.deleteFile(path)) {
                        deletedTrackIds += trackId
                    }
                }

                // if some tracks have not been deleted, rewrite delete clause.
                if (deletedTrackIds.size < ids.size) {
                    whereClause = buildInTrackIdClause(deletedTrackIds.size)
                    whereArgs = Array(deletedTrackIds.size) { deletedTrackIds[it].toString() }
                }

                // Delete track information from the MediaStore.
                // Only tracks whose file have been successfully deleted are removed.
                resolver.delete(Media.EXTERNAL_CONTENT_URI, whereClause, whereArgs)
            } ?: 0
        }

        return DeleteTracksResult.Deleted(deleteCount)
    }

    private fun buildInTrackIdClause(paramCount: Int) = buildString {
        append(Media._ID).append(" IN ")
        CharArray(paramCount) { '?' }.joinTo(this, ",", "(", ")")
    }

}