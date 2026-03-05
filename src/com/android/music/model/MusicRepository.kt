package com.android.music.model

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import com.android.music.R

class MusicRepository private constructor(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        @Volatile private var instance: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository =
            instance ?: synchronized(this) {
                instance ?: MusicRepository(context).also { instance = it }
            }
    }

    fun getGenreId(name: String): Long? {
        val c = appContext.contentResolver.query(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID),
            "${MediaStore.Audio.Genres.NAME} = ?",
            arrayOf(name),
            null
        )

        c?.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return null
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadSongs(album: String?, artist: String?, genre: String?): MutableList<MediaItem> {
        Log.d("ALBUM_DEBUG", "ALBUM = $album")
        Log.d("ARTIST_DEBUG", "ARTIST = $artist")
        Log.d("GENRE_DEBUG", "GENRE = $genre")
        val mediaItems = mutableListOf<MediaItem>()

        val resolver = appContext.contentResolver
        var baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        if (genre != null) {
            val genreId = getGenreId(genre) ?: return mutableListOf()
            baseUri = MediaStore.Audio.Genres.Members.getContentUri(
                "external",
                genreId
            )
        }

        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        )
        val args = mutableListOf("Music/%")

        if (album != null) {
            selectionParts.add("${MediaStore.Audio.Media.ALBUM} = ?")
            args.add(album)
        } else if (artist != null) {
            selectionParts.add("${MediaStore.Audio.Media.ARTIST} = ?")
            args.add(artist)
        }

        val selection = selectionParts.joinToString(" AND ")
        Log.d("GENRE_DEBUG", "Members URI = $baseUri")
        val cursor = resolver.query(
            baseUri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            selection,
            args.toTypedArray(),
            MediaStore.Audio.Media.TITLE
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(appContext, uri)
                val title = resolveTitle(appContext, uri, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                val albumArt = mmr.embeddedPicture
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: appContext.getString(R.string.unknown_artist_name)
                mediaItems.add(MediaItem.Builder()
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
        return mediaItems
    }

    fun searchSongs(query: String): MutableList<MediaItem> {
        Log.d("QUERY_DEBUG", "QUERY = $query")
        val mediaItems = mutableListOf<MediaItem>()

        val resolver = appContext.contentResolver
        val baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        )
        val args = mutableListOf("Music/%")

        selectionParts.add("${MediaStore.Audio.Media.TITLE} LIKE ? COLLATE NOCASE")
        args.add("%$query%")

        val selection = selectionParts.joinToString(" AND ")
        val cursor = resolver.query(
            baseUri,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE
            ),
            selection,
            args.toTypedArray(),
            MediaStore.Audio.Media.TITLE
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(appContext, uri)
                val title = resolveTitle(appContext, uri, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                val albumArt = mmr.embeddedPicture
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: appContext.getString(R.string.unknown_artist_name)
                mediaItems.add(MediaItem.Builder()
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
        return mediaItems
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadAlbums(album: String?): MutableList<MediaItem> {
        if (album != null) {
            return loadSongs(album, null, null)
        }
        val albums = mutableMapOf<String, MediaItem>()
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        )
        val args = mutableListOf("Music/%")

        val selection = selectionParts.joinToString(" AND ")
        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.RELATIVE_PATH
            ),
            selection,
            args.toTypedArray(),
            MediaStore.Audio.Media.ALBUM
        )

        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                val album = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                    ?: appContext.getString(R.string.unknown_album_name)
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    ?: appContext.getString(R.string.unknown_artist_name)

                // Already emitted this album? Skip — we only need one representative
                if (albums.containsKey(album)) continue

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(appContext, uri)

                val albumArt = mmr.embeddedPicture
                mmr.release()

                val item = MediaItem.Builder()
                    .setMediaId("album:$album")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(album)
                            .setArtist(artist)
                            .setArtworkData(albumArt, null)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()

                albums[album] = item
            }
        }

        return albums.values.toMutableList()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadArtists(artist: String?): MutableList<MediaItem> {
        if (artist != null) {
            return loadSongs(null, artist, null)
        }
        val artists = mutableMapOf<String, MediaItem>()
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Music/%")

        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.RELATIVE_PATH
            ),
            selection,
            selectionArgs,
            MediaStore.Audio.Media.ARTIST
        )

        cursor?.use {
            while (it.moveToNext()) {
                val artist = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                    ?: appContext.getString(R.string.unknown_artist_name)

                // Already emitted this artist? Skip — we only need one representative
                if (artists.containsKey(artist)) continue

                val item = MediaItem.Builder()
                    .setMediaId("artist:$artist")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(artist)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()

                artists[artist] = item
            }
        }

        return artists.values.toMutableList()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadGenres(genre: String?): MutableList<MediaItem> {
        if (genre != null) {
            return loadSongs(null, null, genre)
        }
        val genres = mutableMapOf<String, MediaItem>()
        val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("Music/%")

        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.RELATIVE_PATH
            ),
            selection,
            selectionArgs,
            MediaStore.Audio.Media.GENRE
        )

        cursor?.use {
            while (it.moveToNext()) {
                val genre = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE))
                    ?: appContext.getString(R.string.unknown_genre_name)

                // Already emitted this genre? Skip — we only need one representative
                if (genres.containsKey(genre)) continue

                val item = MediaItem.Builder()
                    .setMediaId("genre:$genre")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(genre)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()

                genres[genre] = item
            }
        }

        return genres.values.toMutableList()
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

fun Player.saveQueue(prefs: SharedPreferences, mediaItems: MutableList<MediaItem>, album: String?, artist: String?, genre: String?, query: String?) {
    val joined = mediaItems
        .mapNotNull { it.localConfiguration?.uri?.toString() }
        .joinToString("|")

    prefs.edit {
        putString("queue_uris", joined)
        putInt("queue_index", currentMediaItemIndex)
        putBoolean("queue_shuffle", shuffleModeEnabled)
        putInt("queue_repeat", repeatMode)
        putString("queue_filter_album", album)
        putString("queue_filter_artist", artist)
        putString("queue_filter_genre", genre)
        putString("queue_query", query)
    }
}

fun Player.restoreQueue(ctx: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
    val joined = prefs.getString("queue_uris", null) ?: return
    var index = prefs.getInt("queue_index", 0)
    val shuffle = prefs.getBoolean("queue_shuffle", false)
    val repeat = prefs.getInt("queue_repeat", Player.REPEAT_MODE_OFF)
    val album = prefs.getString("queue_filter_album", null)
    val artist = prefs.getString("queue_filter_artist", null)
    val genre = prefs.getString("queue_filter_genre", null)
    val query = prefs.getString("queue_query", null)

    var items = joined.split("|")
        .filter { it.isNotBlank() }
        .map { MediaItem.fromUri(it.toUri()) }

    if (items.isEmpty()) return

    val itemsFromDB = if (!query.isNullOrBlank()) {
        MusicRepository.getInstance(ctx).searchSongs(query)
    } else {
        MusicRepository.getInstance(ctx).loadSongs(album, artist, genre)
    }
    if (itemsFromDB.isEmpty()) {
        // ok..., what...
        return
    }

    if (itemsFromDB.size != items.size) {
        // ok, so library changed
        index = if (itemsFromDB.contains(items[index].localConfiguration!!.uri)) {
            itemsFromDB.indexOf(items[index].localConfiguration!!.uri)
        } else {
            // ah, right, the saved track is gone, bollocks, reset to track 0
            0
        }
        prefs.edit {
            putString("queue_uris", itemsFromDB
                .mapNotNull { it.localConfiguration?.uri?.toString() }
                .joinToString("|"))
        }
        items = itemsFromDB
    }

    setMediaItems(items, index, 0)
    shuffleModeEnabled = shuffle
    repeatMode = repeat
    prepare()
}

fun MutableList<MediaItem>.contains(uri: Uri): Boolean {
    return indexOf(uri) != -1
}

fun MutableList<MediaItem>.indexOf(uri: Uri): Int {
    for (i in 0 until size) {
        if (get(i).localConfiguration!!.uri.toString() == uri.toString()) {
            return i
        }
    }
    return -1
}
