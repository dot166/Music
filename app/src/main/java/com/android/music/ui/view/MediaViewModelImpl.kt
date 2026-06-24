package com.android.music.ui.view

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import com.android.music.R
import com.android.music.model.MusicRepository
import com.android.music.model.saveQueue
import com.android.music.playback.MusicService
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class MediaViewModelImpl(application: Application) : AndroidViewModel(application), MediaViewModel {
    override var controller by mutableStateOf<Player?>(null)
    override var currentPosition by mutableLongStateOf(0L)
    override var duration by mutableLongStateOf(0L)
    override var isPlaying by mutableStateOf(false)
    override var mediaMetadata by mutableStateOf(MediaMetadata.EMPTY)
    private var repository = MusicRepository.getInstance(application)
    private val _uiState = MutableStateFlow(SongsUiState())
    override val uiState: StateFlow<SongsUiState> = _uiState.asStateFlow()

    override fun loadSongs() {
        val filter = _uiState.value.activeFilter
        val mediaItems: MutableList<MediaItem> = repository.loadSongs(
            if (filter is SongFilter.Album) filter.name else null,
            if (filter is SongFilter.Artist) filter.name else null,
            if (filter is SongFilter.Genre) filter.name else null
        )
        _uiState.update { it.copy(songList = mediaItems) }
    }

    override fun loadAlbums(album: String?) {
        val mediaItems: MutableList<MediaItem> = repository.loadAlbums(album)
        _uiState.update { it.copy(albumList = mediaItems) }
    }

    override fun loadArtists(artist: String?) {
        val mediaItems: MutableList<MediaItem> = repository.loadArtists(artist)
        _uiState.update { it.copy(artistList = mediaItems) }
    }

    override fun loadGenres(genre: String?) {
        val mediaItems: MutableList<MediaItem> = repository.loadGenres(genre)
        _uiState.update { it.copy(genreList = mediaItems) }
    }

    override fun applyFilterAndLoadSongs(album: String?, artist: String?, genre: String?) {
        _uiState.update { it.copy(isLoading = true) }
        if (album != null) {
            _uiState.update { it.copy(activeFilter = SongFilter.Album(album)) }
        } else if (artist != null) {
            _uiState.update { it.copy(activeFilter = SongFilter.Artist(artist)) }
        } else if (genre != null) {
            _uiState.update { it.copy(activeFilter = SongFilter.Genre(genre)) }
        } else {
            _uiState.update { it.copy(activeFilter = SongFilter.None) }
        }
        loadSongs() // Reloads the songsMap repository data instantly
        _uiState.update { it.copy(isLoading = false) }
    }

    override fun playTrack(track: MediaItem) {
        val activeCtrl = controller ?: return
        val items = _uiState.value.songList
        val index = items.indexOf(track)
        if (index == -1) return

        val shuffle = activeCtrl.shuffleModeEnabled
        val repeat = activeCtrl.repeatMode

        activeCtrl.setMediaItems(items, index, 0L)
        activeCtrl.prepare()
        activeCtrl.shuffleModeEnabled = shuffle
        activeCtrl.repeatMode = repeat
        activeCtrl.play()

        saveQueueToPreferences(items)
    }

    override fun shuffleQueue() {
        val activeCtrl = controller ?: return
        val items = _uiState.value.songList
        if (items.isEmpty()) return

        val startIndex = Random.nextInt(items.size)
        val repeat = activeCtrl.repeatMode

        activeCtrl.setMediaItems(items, startIndex, 0L)
        activeCtrl.prepare()
        activeCtrl.shuffleModeEnabled = true
        activeCtrl.repeatMode = repeat
        activeCtrl.play()

        saveQueueToPreferences(items)
    }

    override fun toggleRepeatMode() {
        val activeCtrl = controller ?: return

        // Compute the next loop iteration step exactly like your old code block
        val nextMode = when (activeCtrl.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }

        activeCtrl.repeatMode = nextMode
    }

    override fun toggleShuffleMode() {
        val activeCtrl = controller ?: return

        if (activeCtrl.shuffleModeEnabled) {
            activeCtrl.shuffleModeEnabled = false
        } else {
            // Execute your custom 2 AM queue-mixing handler script
            reshuffle(activeCtrl)
        }
    }

    private fun saveQueueToPreferences(items: List<MediaItem>) {
        val filter = _uiState.value.activeFilter
        val query = _uiState.value.searchQuery
        controller?.saveQueue(
            PreferenceManager.getDefaultSharedPreferences(application),
            items.toMutableList(),
            if (filter is SongFilter.Album) filter.name else null,
            if (filter is SongFilter.Artist) filter.name else null,
            if (filter is SongFilter.Genre) filter.name else null,
            query.ifBlank { null },
        )
    }

    override fun load() {
        _uiState.update { it.copy(isLoading = true) }
        loadSongs()
        loadAlbums()
        loadArtists()
        loadGenres()
        _uiState.update { it.copy(isLoading = false) }
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
                searchResults = repository.searchSongs(query) ?: emptyList()
            )
        }
    }

    override fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    override fun reshuffle(controller: Player) {
        val currentIndex = controller.currentMediaItemIndex
        val positionMs = controller.currentPosition

        val items = buildList {
            for (i in 0 until controller.mediaItemCount) {
                add(controller.getMediaItemAt(i))
            }
        }

        controller.setMediaItems(items, currentIndex, positionMs)
        controller.shuffleModeEnabled = true
        controller.prepare()
    }

    private fun updateState(player: Player) {
        isPlaying = player.isPlaying
        duration = player.duration.coerceAtLeast(0L)
        mediaMetadata = player.mediaMetadata
    }

    override suspend fun pollPosition() {
        while (true) {
            controller?.let {
                currentPosition = it.currentPosition
            }
            delay(500)
        }
    }

    override fun connectController(context: Context) {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val ctrl = future.get()
            controller = ctrl
            updateState(ctrl)

            ctrl.addListener(object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    updateState(ctrl)
                }
            })
            _uiState.update { it.copy(isControllerReady = true) }
        }, MoreExecutors.directExecutor())
    }

    override fun pickerOnSearchQueryChanged(query: String, baseUri: Uri) {
        val mediaItems = mutableListOf<MediaItem>()
        val cursor = application.contentResolver.query(
            baseUri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            "${MediaStore.Audio.Media.TITLE} LIKE ? COLLATE NOCASE",
            mutableListOf("%$query%").toTypedArray(),
            MediaStore.Audio.Media.TITLE
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(application, uri)
                val title = resolveTitle(application, uri, mmr.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_TITLE))
                val albumArt = mmr.embeddedPicture
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: application.getString(R.string.unknown_artist_name)
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setArtworkData(albumArt, null)
                                .setIsBrowsable(true)
                                .build()
                        )
                        .build()
                )
                mmr.release()
            }
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                searchResults = mediaItems
            )
        }
    }

    override fun pickerLoadSongs(baseUri: Uri) {
        _uiState.update { it.copy(isLoading = true) }
        val mediaItems = mutableListOf<MediaItem>()
        val cursor = application.contentResolver.query(
            baseUri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            null,
            null,
            MediaStore.Audio.Media.TITLE
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(application, uri)
                val title = resolveTitle(application, uri, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                val albumArt = mmr.embeddedPicture
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: application.getString(R.string.unknown_artist_name)
                mediaItems.add(
                    MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(uri)
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .setArtworkData(albumArt, null)
                                .setIsBrowsable(true)
                                .build()
                        )
                        .build()
                )
                mmr.release()
            }
        }
        _uiState.update { it.copy(songList = mediaItems, isLoading = false) }
    }

    fun resolveTitle(context: Context, uri: Uri, metadataTitle: CharSequence?): String {
        metadataTitle?.toString()?.trim()?.let {
            if (it.isNotEmpty()) return it
        }

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }

        uri.path?.substringAfterLast('/')?.let {
            if (it.isNotBlank()) return it
        }

        return uri.toString()
    }
}

