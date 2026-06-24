package com.android.music.ui.view

sealed interface SongFilter {
    object None : SongFilter
    data class Album(val name: String) : SongFilter
    data class Artist(val name: String) : SongFilter
    data class Genre(val name: String) : SongFilter
}