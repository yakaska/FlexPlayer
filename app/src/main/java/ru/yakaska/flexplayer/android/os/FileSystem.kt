package ru.yakaska.flexplayer.android.os

import java.io.File

interface FileSystem {

    fun deleteFile(filepath: String): Boolean
}

internal class AndroidFileSystem : FileSystem {

    override fun deleteFile(filepath: String): Boolean {
        val file = File(filepath)
        return file.isFile && file.delete()
    }
}