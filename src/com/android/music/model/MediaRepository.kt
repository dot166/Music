package com.android.music.model

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.graphics.createBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.android.music.R
import java.io.ByteArrayOutputStream


class MusicRepository private constructor(context: Context) {

    private val appContext = context.applicationContext

    companion object {
        @Volatile private var instance: MusicRepository? = null

        fun getInstance(context: Context): MusicRepository =
            instance ?: synchronized(this) {
                instance ?: MusicRepository(context).also { instance = it }
            }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun loadSongs(album: String?): MutableList<MediaItem> {
        val mediaItems = mutableListOf<MediaItem>()
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        )
        val args = mutableListOf("Music/%")

        if (album != null) {
            selectionParts.add("${MediaStore.Audio.Media.ALBUM} = ?")
            args.add(album)
        }

        val selection = selectionParts.joinToString(" AND ")
        val cursor = appContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.RELATIVE_PATH
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
    fun loadAlbums(artist: String?): MutableList<MediaItem> {
        val albums = mutableMapOf<String, MediaItem>()
        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        )
        val args = mutableListOf("Music/%")

        if (artist != null) {
            selectionParts.add("${MediaStore.Audio.Media.ARTIST} = ?")
            args.add(artist)
        }

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
    fun loadArtists(): MutableList<MediaItem> {
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

    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        // We ask for the bounds if they have been set as they would be most
        // correct, then we check we are  > 0
        val width = if (!drawable.getBounds().isEmpty) drawable.getBounds()
            .width() else drawable.intrinsicWidth

        val height = if (!drawable.getBounds().isEmpty) drawable.getBounds()
            .height() else drawable.intrinsicHeight

        // Now we check we are > 0
        val bitmap = createBitmap(if (width <= 0) 1 else width, if (height <= 0) 1 else height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

}
