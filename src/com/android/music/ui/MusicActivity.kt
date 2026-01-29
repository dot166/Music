package com.android.music.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.music.R
import com.android.music.playback.MusicService
import com.android.music.ui.fragment.AlbumsFragment
import com.android.music.ui.fragment.ArtistsFragment
import com.android.music.ui.fragment.NowPlayingFragment
import com.android.music.ui.fragment.SongsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.dot166.jlib.app.jActivity


class MusicActivity : jActivity() {
    lateinit var songsFragment: SongsFragment
    lateinit var albumsFragment: AlbumsFragment

    @SuppressLint("Recycle", "NotifyDataSetChanged", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)
        val toolbar = findViewById<Toolbar?>(R.id.actionbar)
        setSupportActionBar(toolbar)

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
                    1 -> {
                        albumsFragment = AlbumsFragment()
                        albumsFragment
                    }
                    2 -> ArtistsFragment()
                    3 -> NowPlayingFragment()
                    else -> error("Tab escaped the universe")
                }
            }
        }

        TabLayoutMediator(tabs, pager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tracks_title)
                1 -> getString(R.string.albums_title)
                2 -> getString(R.string.artists_title)
                3 -> getString(R.string.nowplaying_title)
                else -> "???"
            }

            tab.icon = when (position) {
                0 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_playback_speed)
                1 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_album)
                2 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_artist)
                3 -> getDrawable(androidx.media3.session.R.drawable.media3_icon_play)
                else -> getDrawable(R.drawable.app_music)
            }
        }.attach()
    }
    fun onAlbumSelected(album: String) {
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.currentItem = 0
        songsFragment.showAlbum(album)
    }
    fun onArtistSelected(artist: String) {
        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.currentItem = 1
        albumsFragment.showArtist(artist)
    }

}
