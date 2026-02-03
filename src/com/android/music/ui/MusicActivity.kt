package com.android.music.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.music.R
import com.android.music.playback.MusicService
import com.android.music.ui.fragment.AlbumsFragment
import com.android.music.ui.fragment.ArtistsFragment
import com.android.music.ui.fragment.GenresFragment
import com.android.music.ui.fragment.SongsFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.github.dot166.jlib.app.jActivity


class MusicActivity : jActivity() {
    lateinit var songsFragment: SongsFragment

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val pager = findViewById<ViewPager2>(R.id.pager)
        if (item.itemId == R.id.refresh) {
            when (pager.currentItem) {
                0 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as SongsFragment).refresh()
                1 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as AlbumsFragment).refresh()
                2 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as ArtistsFragment).refresh()
                3 -> (supportFragmentManager.findFragmentByTag("f" + pager.currentItem) as GenresFragment).refresh()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

}
