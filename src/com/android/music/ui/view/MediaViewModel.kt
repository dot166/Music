package com.android.music.ui.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import com.android.music.model.MusicRepository

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val _genreFilter = MutableLiveData<String?>(null)
    private val _albumFilter = MutableLiveData<String?>(null)
    private val _artistFilter = MutableLiveData<String?>(null)
    private val songsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getSongs(): LiveData<MutableList<MediaItem>> {
        return songsMap
    }

    fun loadSongs() {
        val repository = MusicRepository.getInstance(getApplication())
        val mediaItems: MutableList<MediaItem> = repository.loadSongs(_albumFilter.value, _artistFilter.value, _genreFilter.value)
        songsMap.postValue(mediaItems)
    }
    private val albumsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getAlbums(): LiveData<MutableList<MediaItem>> {
        return albumsMap
    }

    fun loadAlbums(album: String? = null) {
        val repository = MusicRepository.getInstance(getApplication())
        val mediaItems: MutableList<MediaItem> = repository.loadAlbums(album)
        albumsMap.postValue(mediaItems)
    }

    fun setAlbumFilter(album: String?) {
        _albumFilter.value = album
    }

    fun getAlbumFilter(): String? {
        return _albumFilter.value
    }

    private val artistsMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getArtists(): LiveData<MutableList<MediaItem>> {
        return artistsMap
    }

    fun loadArtists(artist: String? = null) {
        val repository = MusicRepository.getInstance(getApplication())
        val mediaItems: MutableList<MediaItem> = repository.loadArtists(artist)
        artistsMap.postValue(mediaItems)
    }

    fun setArtistFilter(artist: String?) {
        _artistFilter.value = artist
    }

    fun getArtistFilter(): String? {
        return _artistFilter.value
    }

    private val genreMap: MutableLiveData<MutableList<MediaItem>> =
        MutableLiveData<MutableList<MediaItem>>()

    fun getGenres(): LiveData<MutableList<MediaItem>> {
        return genreMap
    }

    fun loadGenres(genre: String? = null) {
        val repository = MusicRepository.getInstance(getApplication())
        val mediaItems: MutableList<MediaItem> = repository.loadGenres(genre)
        genreMap.postValue(mediaItems)
    }

    fun setGenreFilter(genre: String?) {
        _genreFilter.value = genre
    }

    fun getGenreFilter(): String? {
        return _genreFilter.value
    }

}
