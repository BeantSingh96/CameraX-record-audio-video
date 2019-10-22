package com.knickglobal.videorecord

class AudioModel {
    internal lateinit var aPath: String
    internal lateinit var aName: String
    internal lateinit var aAlbum: String
    internal lateinit var aArtist: String

    fun getaPath(): String {
        return aPath
    }

    fun setaPath(aPath: String) {
        this.aPath = aPath
    }

    fun getaName(): String {
        return aName
    }

    fun setaName(aName: String) {
        this.aName = aName
    }

    fun getaAlbum(): String {
        return aAlbum
    }

    fun setaAlbum(aAlbum: String) {
        this.aAlbum = aAlbum
    }

    fun getaArtist(): String {
        return aArtist
    }

    fun setaArtist(aArtist: String) {
        this.aArtist = aArtist
    }
}