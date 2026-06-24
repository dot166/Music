package com.android.music.ui.view

import androidx.media3.common.MediaItem

data class SongsUiState(
    val songList: List<MediaItem> = emptyList(),
    val albumList: List<MediaItem> = emptyList(),
    val artistList: List<MediaItem> = emptyList(),
    val genreList: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
    val isControllerReady: Boolean = false,
    val activeFilter: SongFilter = SongFilter.None,
    val searchQuery: String = "",
    val searchResults: List<MediaItem> = emptyList()
)