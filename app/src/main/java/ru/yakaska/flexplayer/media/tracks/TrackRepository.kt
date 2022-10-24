package ru.yakaska.flexplayer.media.tracks

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.plus
import ru.yakaska.flexplayer.media.tracks.local.TrackLocalSource

class TrackRepository(
    private val appScope: CoroutineScope,
    private val trackLocalDao: TrackLocalSource,
) {

    // Maybe... refactor?
    private val allTracks: Flow<List<Track>> by lazy {
        trackLocalDao.tracks.map { list ->
            list.map {
                Track(
                    id = it.id,
                    title = it.title,
                    artistId = it.artistId,
                    artist = it.artist,
                    albumId = it.albumId,
                    album = it.album,
                    duration = it.duration,
                    discNumber = it.discNumber,
                    trackNumber = it.trackNumber,
                    mediaUri = it.mediaUri,
                    albumArtUri = it.albumArtUri,
                    availabilityDate = it.availabilityDate,
                    fileSize = it.fileSize,
                )
            }
        }.shareIn(
            scope = appScope + CoroutineName("TracksCache"),
            started = SharingStarted.WhileSubscribed(),
            replay = 1,
        )
    }

    val tracks: Flow<List<Track>>
        get() = allTracks

}

fun TrackRepository.getAlbumTracks(albumId: Long): Flow<List<Track>> =
    tracks.map { allTracks ->
        allTracks
            .filter { it.albumId == albumId }
            .sortedWith(compareBy(Track::discNumber, Track::trackNumber))
    }

fun TrackRepository.getArtistTracks(artistId: Long): Flow<List<Track>> =
    tracks.map { allTracks ->
        allTracks.filter { it.artistId == artistId }
    }
