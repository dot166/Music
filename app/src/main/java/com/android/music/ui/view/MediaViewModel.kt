package com.android.music.ui.view

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.flow.StateFlow

interface MediaViewModel {
    var controller: Player?
    var currentPosition: Long
    var duration: Long
    var isPlaying: Boolean
    var mediaMetadata: MediaMetadata
    val uiState: StateFlow<SongsUiState>
    fun loadSongs()
    fun loadAlbums(album: String? = null)
    fun loadArtists(artist: String? = null)
    fun loadGenres(genre: String? = null)
    fun applyFilterAndLoadSongs(album: String?, artist: String?, genre: String?)
    fun initializeMediaController()
    fun playTrack(track: MediaItem)
    fun shuffleQueue()
    fun toggleRepeatMode()
    fun toggleShuffleMode()
    fun load()
    fun refreshTab(index: Int)
    fun onSearchQueryChanged(query: String)
    fun clearSearch()
    fun reshuffle(controller: Player)
    suspend fun pollPosition()
    fun pickerOnSearchQueryChanged(query: String, baseUri: Uri)
    fun pickerLoadSongs(baseUri: Uri)
}