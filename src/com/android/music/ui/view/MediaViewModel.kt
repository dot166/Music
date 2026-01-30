package com.android.music.ui.view

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import com.android.music.model.MusicRepository

class MediaViewModel() : ViewModel() {
    private val songsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getSongs(): LiveData<MutableList<MediaItem>> {
        return songsMap
    }

    fun loadSongs(application: Application, album: String? = null, genre: String? = null) {
        val repository = MusicRepository.getInstance(application)
        val mediaItems: MutableList<MediaItem> = repository.loadSongs(album, genre)
        songsMap.postValue(mediaItems)
    }
    private val albumsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getAlbums(): LiveData<MutableList<MediaItem>> {
        return albumsMap
    }

    fun loadAlbums(application: Application, artist: String? = null) {
        val repository = MusicRepository.getInstance(application)
        val mediaItems: MutableList<MediaItem> = repository.loadAlbums(artist)
        albumsMap.postValue(mediaItems)
    }
    private val artistsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getArtists(): LiveData<MutableList<MediaItem>> {
        return artistsMap
    }

    fun loadArtists(application: Application) {
        val repository = MusicRepository.getInstance(application)
        val mediaItems: MutableList<MediaItem> = repository.loadArtists()
        artistsMap.postValue(mediaItems)
    }
    private val genreMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getGenres(): LiveData<MutableList<MediaItem>> {
        return genreMap
    }

    fun loadGenres(application: Application) {
        val repository = MusicRepository.getInstance(application)
        val mediaItems: MutableList<MediaItem> = repository.loadGenres()
        genreMap.postValue(mediaItems)
    }
}
