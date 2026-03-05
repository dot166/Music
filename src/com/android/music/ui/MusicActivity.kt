package com.android.music.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.music.R
import com.android.music.model.MusicRepository
import com.android.music.model.saveQueue
import com.android.music.playback.MusicService
import com.android.music.ui.fragment.AlbumsFragment
import com.android.music.ui.fragment.ArtistsFragment
import com.android.music.ui.fragment.GenresFragment
import com.android.music.ui.fragment.SongsFragment
import com.android.music.ui.view.MediaAdapter
import com.google.android.material.search.SearchView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import io.github.dot166.jlib.app.jActivity


class MusicActivity : jActivity() {
    lateinit var songsFragment: SongsFragment
    private lateinit var controller: MediaController

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        startService(Intent(this, MusicService::class.java))
        val tabs = findViewById<TabLayout>(R.id.tabs)
        val pager = findViewById<ViewPager2>(R.id.pager)

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 ->  {
                        songsFragment = SongsFragment()
                        songsFragment
                    }
                    1 -> AlbumsFragment()
                    2 -> ArtistsFragment()
                    3 -> GenresFragment()
                    else -> error("Tab escaped the universe")
                }
            }
        }

        tabs.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // implemented by pager
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // no-op
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                when (pager.currentItem) {
                    0 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as SongsFragment).refresh()
                    1 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as AlbumsFragment).refresh()
                    2 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as ArtistsFragment).refresh()
                    3 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as GenresFragment).refresh()
                }
            }

        })

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tracks_title)
                1 -> getString(R.string.albums_title)
                2 -> getString(R.string.artists_title)
                3 -> getString(R.string.genres_title)
                else -> "???"
            }

            tab.icon = when (position) {
                0 -> getDrawable(R.drawable.music_note_24px)
                1 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_album)
                2 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_artist)
                3 -> getDrawable(R.drawable.genres_24px)
                else -> getDrawable(androidx.media3.session.R.drawable.media3_icon_block)
            }
        }.attach()
        val search = findViewById<SearchView>(R.id.search)
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.setLayoutManager(LinearLayoutManager(this@MusicActivity))
        recyclerView.setItemAnimator(DefaultItemAnimator())
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
                val mediaItems = MusicRepository.getInstance(this@MusicActivity).searchSongs(
                    s as String
                )
                val adapter = MediaAdapter(mediaItems) { item ->
                    val shuffle = controller.shuffleModeEnabled
                    val repeat = controller.repeatMode
                    val startIndex = mediaItems.indexOf(item)
                    if (startIndex == -1) return@MediaAdapter
                    controller.setMediaItems(mediaItems, startIndex, 0L)
                    controller.prepare()
                    controller.shuffleModeEnabled = shuffle
                    controller.repeatMode = repeat
                    controller.play()
                    controller.saveQueue(
                        PreferenceManager.getDefaultSharedPreferences(this@MusicActivity),
                        mediaItems, null, null, null, s
                    )
                }
                recyclerView.adapter = adapter
            }

        })
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))

        val future = MediaController.Builder(this, token)
            .buildAsync()

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(ctrl: MediaController) {
                    controller = ctrl
                }

                override fun onFailure(t: Throwable) {
                    Log.e("TAG", "Failed to build MediaController", t)
                }
            },
            MoreExecutors.directExecutor()
        )
    }
    fun onAlbumSelected(album: String) {
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.currentItem = 0
        songsFragment.showAlbum(album)
    }
    fun onGenreSelected(genre: String) {
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.currentItem = 0
        songsFragment.showGenre(genre)
    }
    fun onArtistSelected(artist: String) {
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.currentItem = 0
        songsFragment.showArtist(artist)
    }

}
