package com.android.music.ui.fragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.android.music.R
import com.android.music.playback.MusicService
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors


class NowPlayingFragment: Fragment() {

    private lateinit var controller: MediaController

    var mHandled: Handler = Handler(Looper.getMainLooper())
    lateinit var layoutMain: ConstraintLayout
    private lateinit var mSeekBar: Slider

    private var mSeeking = false
    private var mUiPaused = true
    private var mDuration = 0L

    private val updateThread: Runnable = object : Runnable {
        override fun run() {
            if (!controller.isConnected) {
                // uh oh, service died
                mHandled.postDelayed({
                    activity!!.recreate() // quick, rebuild UI to start service again.
                }, 2000)
                return  // just in case
            }
            updatePlayPause()
            layoutMain.findViewById<TextView>(R.id.now_playing_title)!!.text = controller.getMediaMetadata().title
            layoutMain.findViewById<TextView>(R.id.now_playing_artist)!!.text = controller.getMediaMetadata().artist
            val artBytes = controller.mediaMetadata.artworkData
            if (artBytes != null) {
                val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                layoutMain.findViewById<ImageView>(R.id.imageView)!!.setImageBitmap(bitmap)
            } else {
                layoutMain.findViewById<ImageView>(R.id.imageView)!!.setImageResource(R.drawable.albumart_mp_unknown)
            }
            mDuration = controller.duration.coerceAtLeast(0L)
            if (mDuration == 0L) {
                mDuration = 1L
            }
            mSeekBar.valueTo = mDuration.toFloat()
            if (!mSeeking && mDuration > 0) {
                val pos = controller.currentPosition.toFloat()
                val clamped = pos.coerceIn(0f, mSeekBar.valueTo)
                mSeekBar.value = clamped
            }
            mHandled.post(updateThread)
        }
    }

    private fun updatePlayPause() {
        val b: MaterialButton? = layoutMain.findViewById<MaterialButton>(R.id.button6)
        if (b != null) {
            if (controller.isPlaying) {
                b.setIconResource(androidx.media3.session.R.drawable.media3_icon_pause)
            } else {
                b.setIconResource(androidx.media3.session.R.drawable.media3_icon_play)
            }
        }
    }


    @SuppressLint("Recycle", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        val token = SessionToken(requireContext(), ComponentName(requireContext(), MusicService::class.java))

        val future = MediaController.Builder(requireContext(), token)
            .buildAsync()
        layoutMain = view.findViewById<ConstraintLayout>(R.id.layoutMain)
        layoutMain.findViewById<TextView>(R.id.now_playing_title)!!.isSelected = true
        layoutMain.findViewById<TextView>(R.id.now_playing_artist)!!.isSelected = true
        mSeekBar = layoutMain.findViewById<Slider>(R.id.seekBar)
        if (mDuration == 0L) {
            mDuration = 1L
        }

        Futures.addCallback(
            future,
            object : FutureCallback<MediaController> {
                override fun onSuccess(ctrl: MediaController) {
                    controller = ctrl

                    mDuration = controller.duration.coerceAtLeast(0L)
                    if (mDuration == 0L) {
                        mDuration = 1L
                    }
                    if (mDuration > 0) {
                        mSeekBar.valueTo = mDuration.toFloat()
                        if (!mSeeking) {
                            mSeekBar.value = controller.currentPosition.toFloat()
                        }
                    }

                    mSeekBar.addOnChangeListener(mSeekListener)
                    mSeekBar.addOnSliderTouchListener(mTouchListener)
                    layoutMain.findViewById<MaterialButton>(R.id.button6).setOnClickListener { v -> playPauseClicked(v) }
                    mHandled.post(updateThread)
                }

                override fun onFailure(t: Throwable) {
                    Log.e("TAG", "Failed to build MediaController", t)
                }
            },
            MoreExecutors.directExecutor()
        )
        return view
    }

    override fun onPause() {
        super.onPause()
        mUiPaused = true
    }

    override fun onResume() {
        super.onResume()
        mUiPaused = false
    }

    private val mTouchListener: Slider.OnSliderTouchListener = object : Slider.OnSliderTouchListener {
        override fun onStartTrackingTouch(bar: Slider) {
            mSeeking = true
        }

        override fun onStopTrackingTouch(bar: Slider) {
            mSeeking = false
        }
    }

    private val mSeekListener: Slider.OnChangeListener = object : Slider.OnChangeListener {
        override fun onValueChange(bar: Slider, progress: Float, fromuser: Boolean) {
            if (!fromuser) return

            val target = progress
                .toLong()
                .coerceIn(0L, controller.duration)

            controller.seekTo(target)
        }
    }

    fun playPauseClicked(v: View?) {
        when {
            controller.playbackState == Player.STATE_ENDED -> {
                controller.seekTo(0)
                controller.play()
            }
            controller.isPlaying -> {
                controller.pause()
            }
            else -> {
                controller.play()
            }
        }
        updatePlayPause()
    }
}