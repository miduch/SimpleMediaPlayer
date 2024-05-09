package com.rcudev.player_service.service

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.rcudev.player_service.service.notification.SimpleMediaNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@AndroidEntryPoint
class SimpleMediaService : MediaSessionService() {

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    private var mediaSession: MediaSession? = null
    private var notificationManager: SimpleMediaNotificationManager? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        println("SimpleMediaService.onCreate")
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val player = ExoPlayer.Builder(appContext)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setTrackSelector(DefaultTrackSelector(appContext))
                .build()

        notificationManager = SimpleMediaNotificationManager(
            context = appContext,
            player = player
        )

        mediaSession = MediaSession.Builder(appContext, player).build()
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("SimpleMediaService.onStartCommand")
        val session = mediaSession
        if (session != null) {
            println("SimpleMediaService.onStartCommand - startNotificationService")
            notificationManager?.startNotificationService(
                mediaSessionService = this,
                mediaSession = session
            )
        }

        return super.onStartCommand(intent, flags, startId)
    }

    // The user dismissed the app from the recent tasks
    override fun onTaskRemoved(rootIntent: Intent?) {
        println("SimpleMediaService.onTaskRemoved")
        val player = mediaSession?.player
        if (player == null ||
            !player.playWhenReady ||
            player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            println("SimpleMediaService.onTaskRemoved - stopSelf")
            // Stop the service if not playing, continue playing in the background otherwise.
            stopSelf()
        }
    }

    override fun onDestroy() {
        println("SimpleMediaService.onDestroy")
        mediaSession?.run {
            if (player.playbackState != Player.STATE_IDLE) {
                player.seekTo(0)
                player.playWhenReady = false
                player.stop()
            }
            println("SimpleMediaService.onDestroy - release player and session")
            player.release()
            release()
            mediaSession = null
            notificationManager = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession
}
