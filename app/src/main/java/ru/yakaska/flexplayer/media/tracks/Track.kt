package ru.yakaska.flexplayer.media.tracks

// Domain model
data class Track(

    val id: Long,

    val title: String,

    val artistId: Long,

    val artist: String,

    val albumId: Long,

    val album: String,

    val duration: Long,

    val discNumber: Int,

    val trackNumber: Int,

    val mediaUri: String,

    val albumArtUri: String?,

    val availabilityDate: Long,

    val fileSize: Long,

    )
