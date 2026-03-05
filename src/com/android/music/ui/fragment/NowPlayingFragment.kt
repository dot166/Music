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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.android.music.R
import com.android.music.playback.MusicService
import com.google.android.material.button.MaterialButton
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
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (::controller.isInitialized) {
                if (!controller.isConnected) {
                    // uh oh, service died
                    mHandled.postDelayed({
                        activity!!.recreate() // quick, rebuild UI to start service again.
                    }, 2000)
                    return  // just in case
                }
                updatePlayPause()
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
                val posTotalTime = controller.currentPosition
                val posHours = (posTotalTime / (1000 * 60 * 60)).toInt()
                val posMinutes = ((posTotalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                val posSeconds = ((posTotalTime % (1000 * 60)) / 1000).toInt()
                layoutMain.findViewById<TextView>(R.id.seek_bar_position).text =
                    String.format("%02d:%02d:%02d", posHours, posMinutes, posSeconds)
                val durTotalTime = controller.duration
                val durHours = (durTotalTime / (1000 * 60 * 60)).toInt()
                val durMinutes = ((durTotalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
                val durSeconds = ((durTotalTime % (1000 * 60)) / 1000).toInt()
                layoutMain.findViewById<TextView>(R.id.seek_bar_duration).text =
                    String.format("%02d:%02d:%02d", durHours, durMinutes, durSeconds)
            }
            mHandled.postDelayed(updateThread, 100)
        }
    }

    private fun updatePlayPause() {
        val b: MaterialButton? = layoutMain.findViewById(R.id.button6)
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
        layoutMain = view.findViewById(R.id.layoutMain)
        layoutMain.findViewById<TextView>(R.id.now_playing_title)!!.isSelected = true
        layoutMain.findViewById<TextView>(R.id.now_playing_artist)!!.isSelected = true
        mSeekBar = layoutMain.findViewById(R.id.seekBar)
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
                    layoutMain.findViewById<MaterialButton>(R.id.button4).setOnClickListener { v ->
                        controller.repeatMode = when (controller.repeatMode) {
                            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
                            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
                            REPEAT_MODE_ONE -> REPEAT_MODE_OFF
                            else -> REPEAT_MODE_OFF
                        }
                        v.isActivated = controller.repeatMode != REPEAT_MODE_OFF
                        (v as MaterialButton).setIconResource(
                            when (controller.repeatMode) {
                                REPEAT_MODE_OFF -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                                REPEAT_MODE_ALL -> androidx.media3.session.R.drawable.media3_icon_repeat_all
                                REPEAT_MODE_ONE -> androidx.media3.session.R.drawable.media3_icon_repeat_one
                                else -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                            }
                        )
                    }
                    layoutMain.findViewById<MaterialButton>(R.id.button5).setOnClickListener { _ ->
                        controller.seekToPreviousMediaItem()
                    }
                    layoutMain.findViewById<MaterialButton>(R.id.button6).setOnClickListener { _ ->
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
                    layoutMain.findViewById<MaterialButton>(R.id.button7).setOnClickListener { _ ->
                        controller.seekToNextMediaItem()
                    }
                    layoutMain.findViewById<MaterialButton>(R.id.button8).setOnClickListener { v ->
                        if (controller.shuffleModeEnabled) {
                            controller.shuffleModeEnabled = false
                        } else {
                            reshuffle(controller)
                        }
                        v.isActivated = controller.shuffleModeEnabled
                        (v as MaterialButton).setIconResource(
                            when (controller.shuffleModeEnabled) {
                                true -> androidx.media3.session.R.drawable.media3_icon_shuffle_on
                                false -> androidx.media3.session.R.drawable.media3_icon_shuffle_off
                            }
                        )
                    }
                    controller.addListener(object : Player.Listener {
                        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                            val artBytes = mediaMetadata.artworkData
                            if (artBytes != null) {
                                val options = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)

                                options.inSampleSize = calculateSampleSize(options)
                                options.inJustDecodeBounds = false

                                val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)
                                layoutMain.findViewById<ImageView>(R.id.imageView)!!.setImageBitmap(bitmap)
                            } else {
                                layoutMain.findViewById<ImageView>(R.id.imageView)!!.setImageResource(R.drawable.def_art)
                            }
                            layoutMain.findViewById<TextView>(R.id.now_playing_title)!!.text = mediaMetadata.title
                            layoutMain.findViewById<TextView>(R.id.now_playing_artist)!!.text = mediaMetadata.artist
                        }

                        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                            layoutMain.findViewById<MaterialButton>(R.id.button8).isActivated = shuffleModeEnabled
                            layoutMain.findViewById<MaterialButton>(R.id.button8).setIconResource(
                                when (shuffleModeEnabled) {
                                    true -> androidx.media3.session.R.drawable.media3_icon_shuffle_on
                                    false -> androidx.media3.session.R.drawable.media3_icon_shuffle_off
                                }
                            )
                        }

                        override fun onRepeatModeChanged(repeatMode: Int) {
                            layoutMain.findViewById<MaterialButton>(R.id.button4).isActivated = repeatMode != REPEAT_MODE_OFF
                            layoutMain.findViewById<MaterialButton>(R.id.button4).setIconResource(
                                when (repeatMode) {
                                    REPEAT_MODE_OFF -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                                    REPEAT_MODE_ALL -> androidx.media3.session.R.drawable.media3_icon_repeat_all
                                    REPEAT_MODE_ONE -> androidx.media3.session.R.drawable.media3_icon_repeat_one
                                    else -> androidx.media3.session.R.drawable.media3_icon_repeat_off
                                }
                            )
                        }
                    })
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
        mHandled.removeCallbacks(updateThread)
        mUiPaused = true
    }

    override fun onResume() {
        super.onResume()
        mHandled.post(updateThread)
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
    fun reshuffle(controller: MediaController) {
        val currentIndex = controller.currentMediaItemIndex
        val positionMs = controller.currentPosition

        val items = buildList {
            for (i in 0 until controller.mediaItemCount) {
                add(controller.getMediaItemAt(i))
            }
        }

        controller.setMediaItems(items, currentIndex, positionMs)
        controller.shuffleModeEnabled = true
        controller.prepare()
    }

    private fun calculateSampleSize(
        options: BitmapFactory.Options
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > 512 || width > 512) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= 512 &&
                (halfWidth / inSampleSize) >= 512) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

}