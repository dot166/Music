package com.android.music.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.android.music.R
import com.android.music.playback.MusicService
import com.android.music.ui.view.MediaViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.dot166.jlib.app.SettingsLibComposeTheme
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.utils.DateUtils.formatTime
import kotlinx.coroutines.launch

class MusicActivity : jActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, MusicService::class.java))
        setContent {
            SettingsLibComposeTheme {
                val viewModel: MediaViewModel = viewModel()
                val scaffoldState = rememberBottomSheetScaffoldState(
                    bottomSheetState = rememberStandardBottomSheetState(
                        initialValue = SheetValue.PartiallyExpanded
                    )
                )
                if (viewModel.uiState.collectAsState().value.isControllerReady) {
                    BottomSheetScaffold(
                        scaffoldState = scaffoldState,
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(text = title.toString())
                                },
                            )
                        },
                        sheetPeekHeight = 80.dp,
                        sheetContent = {
                            val controller = viewModel.controller

                            LaunchedEffect(Unit) {
                                viewModel.pollPosition()
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val artBytes = viewModel.mediaMetadata.artworkData
                                if (artBytes != null) {
                                    AsyncImage(
                                        model = artBytes,
                                        contentDescription = "Album Art",
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(id = R.drawable.def_art),
                                        error = painterResource(id = R.drawable.def_art),
                                        modifier = Modifier
                                            .size(300.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceColorAtElevation(
                                                    3.dp
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = viewModel.mediaMetadata.title?.toString()
                                        ?: "Unknown",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = viewModel.mediaMetadata.artist?.toString()
                                        ?: stringResource(
                                            R.string.unknown_artist_name
                                        ), style = MaterialTheme.typography.bodyLarge
                                )

                                Spacer(modifier = Modifier.height(32.dp))

                                Slider(
                                    value = viewModel.currentPosition.toFloat(),
                                    valueRange = 0f..viewModel.duration.toFloat().coerceAtLeast(1f),
                                    onValueChange = { controller?.seekTo(it.toLong()) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formatTime(viewModel.currentPosition))
                                    Text(formatTime(viewModel.duration))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isRepeatActive =
                                        viewModel.controller!!.repeatMode != Player.REPEAT_MODE_OFF
                                    val repeatIconRes = when (viewModel.controller!!.repeatMode) {
                                        Player.REPEAT_MODE_OFF -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                                        Player.REPEAT_MODE_ALL -> androidx.media3.session.R.drawable.media3_icon_repeat_all
                                        Player.REPEAT_MODE_ONE -> androidx.media3.session.R.drawable.media3_icon_repeat_one
                                        else -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                                    }

                                    IconToggleButton(
                                        checked = isRepeatActive,
                                        onCheckedChange = { viewModel.toggleRepeatMode() },
                                        colors = IconButtonDefaults.iconToggleButtonColors(
                                            checkedContentColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(id = repeatIconRes),
                                            contentDescription = null
                                        )
                                    }

                                    IconButton(onClick = { controller?.seekBack() }) {
                                        Icon(
                                            Icons.Default.SkipPrevious,
                                            stringResource(R.string.skip_previous)
                                        )
                                    }

                                    FilledIconButton(
                                        onClick = { if (viewModel.isPlaying) controller?.pause() else controller?.play() },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            stringResource(R.string.play_pause)
                                        )
                                    }

                                    IconButton(onClick = { controller?.seekForward() }) {
                                        Icon(
                                            Icons.Default.SkipNext,
                                            stringResource(R.string.skip_next)
                                        )
                                    }

                                    val shuffleIconRes =
                                        if (viewModel.controller!!.shuffleModeEnabled) {
                                            androidx.media3.session.R.drawable.media3_icon_shuffle_on
                                        } else {
                                            androidx.media3.session.R.drawable.media3_icon_shuffle_off
                                        }

                                    IconToggleButton(
                                        checked = viewModel.controller!!.shuffleModeEnabled,
                                        onCheckedChange = { viewModel.toggleShuffleMode() },
                                        colors = IconButtonDefaults.iconToggleButtonColors(
                                            checkedContentColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(id = shuffleIconRes),
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        },
                        sheetDragHandle = { BottomSheetDefaults.DragHandle() }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding),
                        ) {
                            MusicPlayerMainScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    fun MusicPlayerMainScreen(viewModel: MediaViewModel) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val pagerState = rememberPagerState(pageCount = { 4 })
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.load()
        }

        val tabs = listOf(
            R.string.tracks_title to R.drawable.music_note_24px,
            R.string.albums_title to androidx.media3.session.R.drawable.media3_icon_album,
            R.string.artists_title to androidx.media3.session.R.drawable.media3_icon_artist,
            R.string.genres_title to R.drawable.genres_24px
        )

        // Bridge page changes back to state for manual re-selections
        //LaunchedEffect(pagerState.currentPage) {
        //    viewModel.refreshTab(pagerState.currentPage)
        //}

        Column(modifier = Modifier.fillMaxSize()) {
            // Replaces the material M3 SearchView layout entirely
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearch = {},
                active = uiState.searchQuery.isNotEmpty(),
                onActiveChange = { if (!it) viewModel.clearSearch() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                // Replaces the search RecyclerView + MediaAdapter
                LazyColumn {
                    items(uiState.searchResults) { item ->
                        MediaItemRow(item = item) {
                            viewModel.playTrack(item)
                        }
                    }
                }
            }

            if (uiState.searchQuery.isEmpty()) {
                // Replaces TabLayout + TabLayoutMediator
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, (titleRes, iconRes) ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                if (pagerState.currentPage == index) {
                                    viewModel.refreshTab(index) // Replaces your onTabReselected fragment refresh logic
                                } else {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                }
                            },
                            text = { Text(stringResource(id = titleRes)) },
                            icon = { Icon(painterResource(id = iconRes), contentDescription = null) }
                        )
                    }
                }

                // Replaces ViewPager2 + FragmentStateAdapter
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        // Replaces SongsFragment
                        0 -> MetadataTabScreen(
                            isLoading = uiState.isLoading,
                            itemsList = uiState.songList,
                            onItemClick = { item, _ -> viewModel.playTrack(item) }
                        )

                        // Replaces AlbumsFragment
                        1 -> MetadataTabScreen(
                            isLoading = uiState.isLoading,
                            itemsList = uiState.albumList,
                            onItemClick = { _, albumName ->
                                // Apply filter and load songs using your new unified wrapper
                                viewModel.applyFilterAndLoadSongs(album = albumName, artist = null, genre = null)
                                // Animate smoothly back to the Tracks tab instantly
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )

                        // Replaces ArtistsFragment
                        2 -> MetadataTabScreen(
                            isLoading = uiState.isLoading,
                            itemsList = uiState.artistList,
                            onItemClick = { _, artistName ->
                                viewModel.applyFilterAndLoadSongs(album = null, artist = artistName, genre = null)
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )

                        // Replaces GenresFragment
                        3 -> MetadataTabScreen(
                            isLoading = uiState.isLoading,
                            itemsList = uiState.genreList,
                            onItemClick = { _, genreName ->
                                viewModel.applyFilterAndLoadSongs(album = null, artist = null, genre = genreName)
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                    }
                }

            }
        }
    }

    @Composable
    fun MetadataTabScreen(
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
                        // Reusable generic display item row template
                        MediaItemRow(item = item) {
                            // Extract the metadata string natively on click interaction
                            val metadataTitle = item.mediaMetadata.title?.toString() ?: ""
                            onItemClick(item, metadataTitle)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MediaItemRow(
        item: MediaItem,
        onClick: () -> Unit
    ) {
        val title = item.mediaMetadata.title?.toString() ?: "Unknown Title"
        val artist = item.mediaMetadata.artist?.toString() ?: ""
        val artBytes = item.mediaMetadata.artworkData

        // Safe, memory-isolated image source routing
        val imageModel = remember(item.mediaId) {
            artBytes ?: R.drawable.def_art // Pass byte array or default drawable ID straight to Coil
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .clickable { onClick() } // Replaces viewHolder.itemView.setOnClickListener
                .padding(
                    start = dimensionResource(id = R.dimen.spacing_medium),
                    top = dimensionResource(id = R.dimen.spacing_mid_medium),
                    end = dimensionResource(id = R.dimen.spacing_mid_medium),
                    bottom = dimensionResource(id = R.dimen.spacing_mid_medium)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replaces your raw BitmapFactory.decodeByteArray thread blocking hooks entirely
            AsyncImage(
                model = imageModel,
                contentDescription = "Album Art",
                contentScale = ContentScale.Crop,
                placeholder = rememberDrawablePainter(getDrawable(R.drawable.def_art)),
                error = rememberDrawablePainter(getDrawable(R.drawable.def_art)),
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
                // Replaces viewHolder.title.text + viewHolder.title.isSelected = true
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE) // Natively forces persistent scrolling loop
                )

                // Replaces viewHolder.artist.text + viewHolder.artist.isSelected = true
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


}
