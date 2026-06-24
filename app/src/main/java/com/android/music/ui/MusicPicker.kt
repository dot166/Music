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
package com.android.music.ui

import android.content.ClipData
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.android.music.R
import com.android.music.ui.components.MediaItemRow
import com.android.music.ui.components.TabScreen
import com.android.music.ui.view.MediaViewModel
import com.android.music.ui.view.MediaViewModelImpl
import com.android.music.ui.view.StubViewModel
import com.android.settingslib.spa.framework.theme.SettingsTheme
import io.github.dot166.jlib.app.jActivity

class MusicPicker : jActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val baseUri = if (Intent.ACTION_GET_CONTENT == intent.action) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            intent.data ?: MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        setContent {
            SettingsTheme {
                val viewModel: MediaViewModelImpl = viewModel()
                MusicPickerMainScreen(viewModel, baseUri)
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
        SettingsTheme {
            MusicPickerMainScreen(StubViewModel(), MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MusicPickerMainScreen(viewModel: MediaViewModel, baseUri: Uri) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        Scaffold(
            topBar = {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.pickerOnSearchQueryChanged(it, baseUri) },
                            expanded = uiState.searchQuery.isNotEmpty(),
                            onExpandedChange = { if (!it) viewModel.clearSearch() },
                            onSearch = {},
                            placeholder = { Text(stringResource(R.string.searchbar_hint)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    stringResource(R.string.searchbar_hint)
                                )
                            },
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
                                val songUri = item.localConfiguration?.uri ?: return@MediaItemRow
                                val resultIntent = Intent().apply {
                                    data = songUri
                                    clipData = ClipData.newRawUri("Selected Song", songUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding),
            ) {
                LaunchedEffect(Unit) {
                    viewModel.pickerLoadSongs(baseUri)
                }
                if (uiState.searchQuery.isEmpty()) {
                    TabScreen(
                        isLoading = uiState.isLoading,
                        itemsList = uiState.songList,
                        onItemClick = { item, _ ->
                            val songUri = item.localConfiguration?.uri ?: return@TabScreen
                            val resultIntent = Intent().apply {
                                data = songUri
                                clipData = ClipData.newRawUri("Selected Song", songUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}