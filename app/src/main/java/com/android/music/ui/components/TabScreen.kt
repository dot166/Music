package com.android.music.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem

@Composable
fun TabScreen(
    isLoading: Boolean,
    itemsList: List<MediaItem>,
    onItemClick: (MediaItem, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(itemsList) { item ->
                    MediaItemRow(item = item) {
                        val metadataTitle = item.mediaMetadata.title?.toString() ?: ""
                        onItemClick(item, metadataTitle)
                    }
                }
            }
        }
    }
}