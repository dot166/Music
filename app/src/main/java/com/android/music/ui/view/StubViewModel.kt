package com.android.music.ui.view

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.test.utils.FakePlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * this is an implementation of [MediaViewModel] for [androidx.compose.ui.tooling.preview.Preview] functions, for the actual view model, please use [MediaViewModelImpl]
 */
@UnstableApi
class StubViewModel : MediaViewModel {
    override var controller by mutableStateOf<Player?>(FakePlayer())
    override var currentPosition by mutableLongStateOf(0L)
    override var duration by mutableLongStateOf(1000L)
    override var isPlaying by mutableStateOf(false)
    override var mediaMetadata by mutableStateOf(MediaMetadata.EMPTY)
    private val _uiState = MutableStateFlow(SongsUiState(isControllerReady = true, isLoading = false))
    override val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    override fun loadSongs() {
        val list = mutableListOf(MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle("ローリンガール").setArtist("wowaka feat. 初音ミク").build()).build())
        _uiState.update { it.copy(songList = list) }
    }

    override fun loadAlbums(album: String?) {
        val list = mutableListOf(MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle("アンハッピーリフレイン").setArtist("wowaka feat. 初音ミク").build()).build())
        _uiState.update { it.copy(albumList = list) }
    }

    override fun loadArtists(artist: String?) {
        val list = mutableListOf(MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle("wowaka feat. 初音ミク").build()).build())
        _uiState.update { it.copy(artistList = list) }
    }

    override fun loadGenres(genre: String?) {
        val list = mutableListOf(MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle("Vocaloid").build()).build())
        _uiState.update { it.copy(genreList = list) }
    }

    override fun applyFilterAndLoadSongs(album: String?, artist: String?, genre: String?) {
        // do nothing, unsupported in stub
    }

    override fun playTrack(track: MediaItem) {
        // do nothing, unsupported in stub
    }

    override fun shuffleQueue() {
        // do nothing, unsupported in stub
    }

    override fun toggleRepeatMode() {
        // do nothing, unsupported in stub
    }

    override fun toggleShuffleMode() {
        // do nothing, unsupported in stub
    }

    override fun load() {
        loadSongs()
        loadAlbums()
        loadArtists()
        loadGenres()
    }

    override fun refreshTab(index: Int) {
        when (index) {
            0 -> {
                applyFilterAndLoadSongs(null, null, null)
            }

            1 -> {
                loadAlbums()
            }

            2 -> {
                loadArtists()
            }

            3 -> {
                loadGenres()
            }
        }
    }

    override fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = listOf(MediaItem.Builder().setMediaId("39").setMediaMetadata(MediaMetadata.Builder().setTitle("ローリンガール").setArtist("wowaka feat. 初音ミク").build()).build())
            )
        }
    }

    override fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    override fun reshuffle(controller: Player) {
        // do nothing, unsupported in stub
    }

    override suspend fun pollPosition() {
        // do nothing, unsupported in stub
    }

    override fun connectController(context: Context) {
        // do nothing, unsupported in stub
    }

    override fun pickerOnSearchQueryChanged(query: String, baseUri: Uri) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = listOf(MediaItem.Builder().setMediaId("39").setMediaMetadata(MediaMetadata.Builder().setTitle("ローリンガール").setArtist("wowaka feat. 初音ミク").build()).build())
            )
        }
    }

    override fun pickerLoadSongs(baseUri: Uri) {
        val list = mutableListOf(MediaItem.Builder().setMediaId("1").setMediaMetadata(MediaMetadata.Builder().setTitle("ローリンガール").setArtist("wowaka feat. 初音ミク").build()).build())
        _uiState.update { it.copy(songList = list) }
    }
}
