/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.music

import android.content.ClipData
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.music.model.MusicRepository
import com.android.music.model.saveQueue
import com.android.music.ui.view.MediaAdapter
import com.google.android.material.search.SearchView
import io.github.dot166.jlib.app.jActivity


class MusicPicker : jActivity() {

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        val baseUri = if (Intent.ACTION_GET_CONTENT == intent.action) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            intent.data ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        setContentView(R.layout.activity_pick)
        val mediaItems = mutableListOf<MediaItem>()
        val cursor = contentResolver.query(
            baseUri!!,
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
                mmr.setDataSource(this, uri)
                val title = resolveTitle(this, uri, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                val albumArt = mmr.embeddedPicture
                val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: getString(R.string.unknown_artist_name)
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
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        val adapter = MediaAdapter(mediaItems) { item ->
            val songUri = item.localConfiguration?.uri ?: return@MediaAdapter
            val resultIntent = Intent().apply {
                data = songUri
                clipData = ClipData.newRawUri("Selected Song", songUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        recyclerView.setItemAnimator(DefaultItemAnimator())
        recyclerView.adapter = adapter

        val search = findViewById<SearchView>(R.id.search)
        val recyclerViewSearch: RecyclerView = findViewById(R.id.recycler_view_search)
        recyclerViewSearch.setLayoutManager(LinearLayoutManager(this))
        recyclerViewSearch.setItemAnimator(DefaultItemAnimator())
        search.editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // noop
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // noop
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                val mediaItems = mutableListOf<MediaItem>()
                val cursor = contentResolver.query(
                    baseUri,
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE
                    ),
                    "${MediaStore.Audio.Media.TITLE} LIKE ? COLLATE NOCASE",
                    mutableListOf("%$s%").toTypedArray(),
                    MediaStore.Audio.Media.TITLE
                )
                cursor?.use {
                    while (it.moveToNext()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                        val uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                        )
                        val mmr = MediaMetadataRetriever()
                        mmr.setDataSource(this@MusicPicker, uri)
                        val title = resolveTitle(this@MusicPicker, uri, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                        val albumArt = mmr.embeddedPicture
                        val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?: getString(R.string.unknown_artist_name)
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
                val adapter = MediaAdapter(mediaItems) { item ->
                    val songUri = item.localConfiguration?.uri ?: return@MediaAdapter
                    val resultIntent = Intent().apply {
                        data = songUri
                        clipData = ClipData.newRawUri("Selected Song", songUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
                recyclerViewSearch.adapter = adapter
            }

        })
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