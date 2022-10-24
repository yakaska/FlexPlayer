package ru.yakaska.flexplayer.media.tracks.local

data class LocalTrack(

    val id: Long,

    val title: String,

    val artist: String,

    val album: String,

    val duration: Long,

    val discNumber: Int,

    val trackNumber: Int,

    val mediaUri: String,

    val albumArtUri: String?,

    val availabilityDate: Long,

    val artistId: Long,

    val albumId: Long,

    val fileSize: Long,
)
