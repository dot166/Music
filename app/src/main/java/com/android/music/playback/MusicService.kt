package com.android.music.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.RemoteViews
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
import com.android.music.MediaControlsWidget
import com.android.music.R
import com.android.music.model.restoreQueue
import com.android.music.ui.MusicActivity

class MusicService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaSession

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.restoreQueue(this)

        player.addListener(object: Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                PreferenceManager.getDefaultSharedPreferences(this@MusicService).edit {
                    putInt("queue_index", player.currentMediaItemIndex)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                PreferenceManager.getDefaultSharedPreferences(this@MusicService).edit {
                    putBoolean("queue_shuffle", shuffleModeEnabled)
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                PreferenceManager.getDefaultSharedPreferences(this@MusicService).edit {
                    putInt("queue_repeat", repeatMode)
                }
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
            }
        })
        updateWidget()

        session = MediaSession.Builder(this, player)
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MusicActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return session
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_TOGGLE_PLAY_PAUSE" -> {
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
                updateWidget()
            }
            "ACTION_NEXT" -> {
                player.seekToNextMediaItem()
                updateWidget()
            }
            "ACTION_PREVIOUS" -> {
                player.seekToPreviousMediaItem()
                updateWidget()
            }
            "UPDATE" -> {
                updateWidget()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun updateWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, MediaControlsWidget::class.java)
        )
        Log.d("MusicWidget", ids.size.toString())
        if (ids.isEmpty()) return
        val views = RemoteViews(packageName, R.layout.widget)
        val mediaMetadata = player.mediaMetadata
        val artBytes = mediaMetadata.artworkData
        if (artBytes != null) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size, options)

            options.inSampleSize = calculateSampleSize(options)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeByteArray(
                artBytes,
                0,
                artBytes.size,
                options
            )
            views.setImageViewBitmap(R.id.art, bitmap)
        } else {
            views.setImageViewResource(R.id.art, R.drawable.def_art)
        }
        views.setTextViewText(R.id.line1, mediaMetadata.title ?: "")
        views.setTextViewText(R.id.line2, mediaMetadata.artist ?: "")
        if (player.isPlaying) {
            views.setImageViewResource(R.id.button6, androidx.media3.session.R.drawable.media3_icon_pause)
        } else {
            views.setImageViewResource(R.id.button6, androidx.media3.session.R.drawable.media3_icon_play)
        }
        views.setOnClickPendingIntent(
            R.id.button5,
            PendingIntent.getForegroundService(
                this,
                1,
                Intent(this, MusicService::class.java).apply {
                    action = "ACTION_PREVIOUS"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.button6,
            PendingIntent.getForegroundService(
                this,
                2,
                Intent(this, MusicService::class.java).apply {
                    action = "ACTION_TOGGLE_PLAY_PAUSE"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.button7,
            PendingIntent.getForegroundService(
                this,
                3,
                Intent(this, MusicService::class.java).apply {
                    action = "ACTION_NEXT"
                }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        manager.updateAppWidget(ids, views)
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

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }
}
