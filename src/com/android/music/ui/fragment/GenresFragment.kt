package com.android.music.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.music.R
import com.android.music.playback.MusicService
import com.android.music.ui.MusicActivity
import com.android.music.ui.view.MediaAdapter
import com.android.music.ui.view.MediaViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors

class GenresFragment: Fragment() {

    private lateinit var controller: MediaController
    private var viewModel: MediaViewModel? = null
    var adapter: MediaAdapter? = null

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
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipe_layout)
        progressBar!!.visibility = View.VISIBLE

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(ctrl: MediaController) {
                    controller = ctrl
                    viewModel = ViewModelProvider(this@GenresFragment)[MediaViewModel::class.java]
                    viewModel!!.getGenres()
                        .observe(viewLifecycleOwner, Observer { mediaItems: MutableList<MediaItem>? ->
                            if (mediaItems != null) {
                                val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
                                adapter = MediaAdapter(mediaItems) { item ->
                                    (activity!! as MusicActivity).onGenreSelected(item.mediaMetadata.title.toString())
                                }
                                adapter!!.notifyDataSetChanged()
                                recyclerView.setLayoutManager(LinearLayoutManager(context))
                                recyclerView.setItemAnimator(DefaultItemAnimator())
                                recyclerView.adapter = adapter
                                progressBar.visibility = View.GONE
                                swipeRefreshLayout!!.isRefreshing = false
                            }
                        })
                    viewModel!!.loadGenres(activity!!.application)
                }

                override fun onFailure(t: Throwable) {
                    Log.e("TAG", "Failed to build MediaController", t)
                }
            },
            MoreExecutors.directExecutor()
        )

        swipeRefreshLayout!!.setColorSchemeColors(
            requireContext().obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimary))
                .getColor(0, 0)
        )
        swipeRefreshLayout.canChildScrollUp()
        swipeRefreshLayout.setOnRefreshListener {
            if (adapter != null) {
                adapter!!.musicList.clear()
                adapter!!.notifyDataSetChanged()
            }
            swipeRefreshLayout.isRefreshing = true
            progressBar.visibility = View.VISIBLE
            viewModel!!.loadGenres(requireActivity().application)
        }
        return view
    }
}