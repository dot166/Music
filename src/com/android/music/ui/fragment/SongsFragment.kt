package com.android.music.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.music.R
import com.android.music.model.saveQueue
import com.android.music.playback.MusicService
import com.android.music.ui.view.MediaAdapter
import com.android.music.ui.view.MediaViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors

class SongsFragment: Fragment() {

    private lateinit var controller: MediaController
    private var viewModel: MediaViewModel? = null
    var adapter: MediaAdapter? = null

    fun showAlbum(album: String) {
        viewModel!!.setGenreFilter(null)
        viewModel!!.setArtistFilter(null)
        viewModel!!.setAlbumFilter(album)
        viewModel!!.loadSongs()
    }

    fun showArtist(artist: String) {
        viewModel!!.setGenreFilter(null)
        viewModel!!.setArtistFilter(artist)
        viewModel!!.setAlbumFilter(null)
        viewModel!!.loadSongs()
    }

    fun showGenre(genre: String) {
        viewModel!!.setGenreFilter(genre)
        viewModel!!.setArtistFilter(null)
        viewModel!!.setAlbumFilter(null)
        viewModel!!.loadSongs()
    }

    @SuppressLint("Recycle", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_media, container, false)
        val token = SessionToken(requireContext(), ComponentName(requireContext(), MusicService::class.java))

        val future = MediaController.Builder(requireContext(), token)
            .buildAsync()
        val progressBar = view.findViewById<CircularProgressIndicator>(R.id.progress)
        progressBar!!.visibility = View.VISIBLE

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(ctrl: MediaController) {
                    controller = ctrl
                    viewModel = ViewModelProvider(this@SongsFragment)[MediaViewModel::class.java]
                    viewModel!!.getSongs()
                        .observe(viewLifecycleOwner, Observer { mediaItems: MutableList<MediaItem>? ->
                            if (mediaItems != null) {
                                val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
                                adapter = MediaAdapter(mediaItems) { item ->
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
                                        PreferenceManager.getDefaultSharedPreferences(context!!),
                                        mediaItems, viewModel!!.getAlbumFilter(), viewModel!!.getArtistFilter(), viewModel!!.getGenreFilter(),
                                    )
                                }
                                recyclerView.setLayoutManager(LinearLayoutManager(context))
                                recyclerView.setItemAnimator(DefaultItemAnimator())
                                recyclerView.adapter = adapter
                                progressBar.visibility = View.GONE
                            }
                        })
                    viewModel!!.loadSongs()
                }

                override fun onFailure(t: Throwable) {
                    Log.e("TAG", "Failed to build MediaController", t)
                }
            },
            MoreExecutors.directExecutor()
        )
        return view
    }

    fun refresh() {
        val progressBar = requireView().findViewById<CircularProgressIndicator>(R.id.progress)
        if (adapter != null) {
            adapter!!.musicList.clear()
            adapter!!.notifyDataSetChanged()
        }
        progressBar.visibility = View.VISIBLE
        viewModel!!.setGenreFilter(null)
        viewModel!!.setArtistFilter(null)
        viewModel!!.setAlbumFilter(null)
        viewModel!!.loadSongs()
    }
}