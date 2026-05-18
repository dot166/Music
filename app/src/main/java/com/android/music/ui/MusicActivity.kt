package com.android.music.ui

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.android.music.R
import com.android.music.playback.MusicService
import com.android.music.ui.components.MediaItemRow
import com.android.music.ui.components.TabScreen
import com.android.music.ui.view.MediaViewModel
import com.android.music.ui.view.MediaViewModelImpl
import com.android.music.ui.view.StubViewModel
import io.github.dot166.jlib.app.SettingsLibComposeTheme
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.utils.DateUtils.formatTime
import kotlinx.coroutines.launch

class MusicActivity : jActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, MusicService::class.java))
        setContent {
            SettingsLibComposeTheme {
                val viewModel: MediaViewModelImpl = viewModel()
                MusicPlayerMainScreen(viewModel)
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Composable
    @Preview(
        wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE,
        uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
        device = "id:Nexus 5X" // the screen on the Nexus 5X is close enough to the fold outer screen
    )
    fun Preview() {
        SettingsLibComposeTheme {
            MusicPlayerMainScreen(StubViewModel())
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MusicPlayerMainScreen(viewModel: MediaViewModel) {
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded
            )
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        if (viewModel.uiState.collectAsState().value.isControllerReady) {
            BottomSheetScaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                query = uiState.searchQuery,
                                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                                expanded = uiState.searchQuery.isNotEmpty(),
                                onExpandedChange = { if (!it) viewModel.clearSearch() },
                                onSearch = {},
                                placeholder = { Text(stringResource(R.string.searchbar_hint)) },
                                leadingIcon = {Icon(Icons.Default.Search, stringResource(R.string.searchbar_hint))},
                            )
                        },
                        expanded = uiState.searchQuery.isNotEmpty(),
                        onExpandedChange = { if (!it) viewModel.clearSearch() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        LazyColumn {
                            items(uiState.searchResults) { item ->
                                MediaItemRow(item = item) {
                                    viewModel.playTrack(item)
                                }
                            }
                        }
                    }
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

                    LaunchedEffect(pagerState.currentPage) {
                        viewModel.refreshTab(pagerState.currentPage)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.searchQuery.isEmpty()) {
                            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                                tabs.forEachIndexed { index, (titleRes, iconRes) ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            if (pagerState.currentPage == index) {
                                                viewModel.refreshTab(index)
                                            } else {
                                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                        },
                                        text = { Text(stringResource(id = titleRes)) },
                                        icon = { Icon(painterResource(id = iconRes), contentDescription = null) }
                                    )
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                when (page) {
                                    0 -> Scaffold(
                                            floatingActionButton = {
                                                FloatingActionButton(
                                                    onClick = { viewModel.shuffleQueue() },
                                                ) {
                                                    Icon(Icons.Filled.Shuffle, contentDescription = stringResource(R.string.shuffle_displayed_songs))
                                                }
                                            }
                                        ) { padding ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(padding),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TabScreen(
                                                isLoading = uiState.isLoading,
                                                itemsList = uiState.songList,
                                                onItemClick = { item, _ -> viewModel.playTrack(item) }
                                            )
                                        }
                                    }

                                    1 -> TabScreen(
                                        isLoading = uiState.isLoading,
                                        itemsList = uiState.albumList,
                                        onItemClick = { _, albumName ->
                                            viewModel.applyFilterAndLoadSongs(album = albumName, artist = null, genre = null)
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )

                                    2 -> TabScreen(
                                        isLoading = uiState.isLoading,
                                        itemsList = uiState.artistList,
                                        onItemClick = { _, artistName ->
                                            viewModel.applyFilterAndLoadSongs(album = null, artist = artistName, genre = null)
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )

                                    3 -> TabScreen(
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
            }
        }
    }
}
