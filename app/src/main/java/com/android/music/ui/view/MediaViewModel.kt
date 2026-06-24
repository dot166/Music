package com.android.music.ui.view

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.dot166.jlib.compose.model.MediaViewModel
import kotlinx.coroutines.flow.StateFlow

interface MediaViewModel : MediaViewModel {
    override val isRss: Boolean
        get() = false
    val uiState: StateFlow<SongsUiState>
    fun loadSongs()
    fun loadAlbums(album: String? = null)
    fun loadArtists(artist: String? = null)
    fun loadGenres(genre: String? = null)
    fun applyFilterAndLoadSongs(album: String?, artist: String?, genre: String?)
    fun playTrack(track: MediaItem)
    fun shuffleQueue()
    fun load()
    fun refreshTab(index: Int)
    fun onSearchQueryChanged(query: String)
    fun clearSearch()
    fun reshuffle(controller: Player)
    fun pickerOnSearchQueryChanged(query: String, baseUri: Uri)
    fun pickerLoadSongs(baseUri: Uri)
}