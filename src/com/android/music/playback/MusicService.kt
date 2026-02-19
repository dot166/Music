package com.android.music.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.preference.PreferenceManager
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
        })

        session = MediaSession.Builder(this, player)
            .setSessionActivity(PendingIntent.getActivity(this, 0, Intent(this, MusicActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return session
    }

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }
}
