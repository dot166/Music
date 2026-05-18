package com.android.music.ui.components

import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.android.music.R
import com.google.accompanist.drawablepainter.rememberDrawablePainter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaItemRow(
    item: MediaItem,
    onClick: () -> Unit
) {
    val title = item.mediaMetadata.title?.toString() ?: "Unknown Title"
    val artist = item.mediaMetadata.artist?.toString() ?: ""
    val artBytes = item.mediaMetadata.artworkData
    val imageModel = remember(item.mediaId) {
        artBytes ?: R.drawable.def_art
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                start = dimensionResource(id = R.dimen.spacing_medium),
                top = dimensionResource(id = R.dimen.spacing_mid_medium),
                end = dimensionResource(id = R.dimen.spacing_mid_medium),
                bottom = dimensionResource(id = R.dimen.spacing_mid_medium)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = "Album Art",
            contentScale = ContentScale.Crop,
            placeholder = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, R.drawable.def_art)),
            error = rememberDrawablePainter(AppCompatResources.getDrawable(LocalContext.current, R.drawable.def_art)),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.size_touchable_small))
                .clip(RoundedCornerShape(12.dp))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = dimensionResource(id = R.dimen.spacing_medium)),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )

            if (artist.isNotEmpty()) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )
            }
        }
    }
}