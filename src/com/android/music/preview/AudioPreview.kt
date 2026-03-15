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
package com.android.music.preview

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.android.music.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import io.github.dot166.jlib.app.jActivity
import io.github.dot166.jlib.utils.ErrorUtils

/**
 * Dialog that comes up in response to various music-related VIEW intents.
 */
class AudioPreview : jActivity() {
    private lateinit var mTextLine1: TextView
    private lateinit var mTextLine2: TextView
    private lateinit var mSeekBar: Slider
    private lateinit var mSeekBarPos: TextView
    private lateinit var mSeekBarDur: TextView
    private lateinit var player: ExoPlayer

    private var mSeeking = false
    private var mUiPaused = true
    private var mDuration = 0L
    private var mUri: Uri? = null

    private val mProgressRefresher = Handler(Looper.getMainLooper())

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        val intent = intent ?: run {
            finish()
            return
        }

        mUri = intent.data ?: run {
            finish()
            return
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
        setContentView(R.layout.audiopreview)

        mTextLine1 = findViewById(R.id.line1)
        mTextLine2 = findViewById(R.id.line2)
        mSeekBar = findViewById(R.id.seekBar)
        mSeekBarPos = findViewById(R.id.seek_bar_position)
        mSeekBarDur = findViewById(R.id.seek_bar_duration)

        mTextLine1.isSelected = true
        mTextLine2.isSelected = true

        // --- Media3 player ---
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    showPostPrepareUI()
                } else if (state == Player.STATE_ENDED) {
                    mSeekBar.value = mSeekBar.valueTo
                    updatePlayPause()
                }
            }

            override fun onMediaMetadataChanged(metadata: MediaMetadata) {
                val title = resolveTitle(this@AudioPreview, mUri!!, metadata.title)
                mTextLine1.text = title

                val artist = metadata.artist?.toString()
                if (!artist.isNullOrBlank()) {
                    mTextLine2.text = artist
                    mTextLine2.visibility = View.VISIBLE
                } else {
                    mTextLine2.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                ErrorUtils.handle(error, this@AudioPreview, getString(R.string.playback_failed)) {
                    finish()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPause()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mTextLine1.text = resolveTitle(this@AudioPreview, mUri!!, null)
            }
        })

        val mediaItem = MediaItem.fromUri(mUri!!)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onPause() {
        super.onPause()
        mUiPaused = true
        mProgressRefresher.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        mUiPaused = false
    }

    override fun onDestroy() {
        mProgressRefresher.removeCallbacksAndMessages(null)
        player.release()
        super.onDestroy()
    }

    private fun stopPlayback() {
        mProgressRefresher.removeCallbacksAndMessages(null)
        player.release()
    }

    override fun onUserLeaveHint() {
        stopPlayback()
        finish()
        super.onUserLeaveHint()
    }

    private fun showPostPrepareUI() {
        mDuration = player.duration.coerceAtLeast(0L)

        if (mDuration > 0) {
            mSeekBar.valueTo = mDuration.toFloat()
            if (!mSeeking) {
                mSeekBar.value = player.currentPosition.toFloat()
            }
        }

        mSeekBar.addOnChangeListener(mSeekListener)
        mSeekBar.addOnSliderTouchListener(mTouchListener)

        mProgressRefresher.removeCallbacksAndMessages(null)
        mProgressRefresher.postDelayed(ProgressRefresher(), 200)

        updatePlayPause()
    }

    private fun start() {
        player.play()
        mProgressRefresher.postDelayed(ProgressRefresher(), 200)
    }

    inner class ProgressRefresher : Runnable {
        @SuppressLint("DefaultLocale")
        override fun run() {
            if (!mSeeking && mDuration > 0) {
                val pos = player.currentPosition.toFloat()
                val clamped = pos.coerceIn(0f, mSeekBar.valueTo)
                mSeekBar.value = clamped
            }
            val posTotalTime = player.currentPosition
            val posHours = (posTotalTime / (1000 * 60 * 60)).toInt()
            val posMinutes = ((posTotalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
            val posSeconds = ((posTotalTime % (1000 * 60)) / 1000).toInt()
            mSeekBarPos.text = String.format("%02d:%02d:%02d", posHours, posMinutes, posSeconds)
            val durTotalTime = player.duration
            val durHours = (durTotalTime / (1000 * 60 * 60)).toInt()
            val durMinutes = ((durTotalTime % (1000 * 60 * 60)) / (1000 * 60)).toInt()
            val durSeconds = ((durTotalTime % (1000 * 60)) / 1000).toInt()
            mSeekBarDur.text = String.format("%02d:%02d:%02d", durHours, durMinutes, durSeconds)

            mProgressRefresher.removeCallbacksAndMessages(null)
            if (!mUiPaused) {
                mProgressRefresher.postDelayed(this, 200)
            }
        }
    }

    private fun updatePlayPause() {
        val b: MaterialButton? = findViewById(R.id.playpause)
        if (b != null) {
            if (player.isPlaying) {
                b.setIconResource(androidx.media3.session.R.drawable.media3_icon_pause)
            } else {
                b.setIconResource(androidx.media3.session.R.drawable.media3_icon_play)
            }
        }
    }

    fun resolveTitle(context: Context, uri: Uri, metadataTitle: CharSequence?): String {
        metadataTitle?.toString()?.trim()?.let {
            if (it.isNotEmpty()) return it
        }

        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0)
                if (!name.isNullOrBlank()) return name
            }
        }

        uri.path?.substringAfterLast('/')?.let {
            if (it.isNotBlank()) return it
        }

        return uri.toString()
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
                .coerceIn(0L, player.duration)

            player.seekTo(target)
        }
    }

    fun playPauseClicked(v: View?) {
        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0)
                player.play()
            }
            player.isPlaying -> {
                player.pause()
            }
            else -> {
                player.play()
            }
        }
        updatePlayPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    start()
                }
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                }
                updatePlayPause()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND -> return true
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                stopPlayback()
                finish()
                return true
            }
            else -> return super.onKeyDown(keyCode, event)
        }
    }
}