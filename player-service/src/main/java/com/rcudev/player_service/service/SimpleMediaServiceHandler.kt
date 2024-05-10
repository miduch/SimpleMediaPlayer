package com.rcudev.player_service.service

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SimpleMediaServiceHandler @Inject constructor(
    private val appContext: Context,
    private val scope: CoroutineScope,
) : Player.Listener {

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    val simpleMediaState = _simpleMediaState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var job: Job? = null

    init {
        if (!appContext.isServiceRunning(SimpleMediaService::class.java)) {
            println("SimpleMediaServiceHandler.init service not running, start")
            Intent(appContext, SimpleMediaService::class.java).also {
                appContext.startForegroundService(it)
            }
        }
    }

    fun connect(
        callBack: (Boolean, Boolean) -> Unit = { _, _ -> },
    ) {
        println("SimpleMediaServiceHandler.connect")
        kotlin.runCatching {
            val sessionToken = SessionToken(
                appContext,
                ComponentName(appContext, SimpleMediaService::class.java)
            )
            controllerFuture = MediaController.Builder(appContext, sessionToken).buildAsync()
            controllerFuture?.addListener(
                {
                    println("SimpleMediaServiceHandler.connect - controllerFuture.get")
                    controller = controllerFuture?.get()
                    controller?.addListener(this@SimpleMediaServiceHandler)
                    val connected = controller?.isConnected ?: false
                    val playing = controller?.isPlaying ?: false
                    callBack(connected, playing)
                    _simpleMediaState.update {
                        SimpleMediaState.Ready(controller?.duration ?: 0)
                    }
                    onIsPlayingChanged(playing)
                },
                MoreExecutors.directExecutor()
            )
        }.onSuccess {
            println("SimpleMediaServiceHandler.connect session token created")
        }.onFailure {
            println("SimpleMediaServiceHandler.connect failed to get handle to controller. err ${it.message}")
            callBack(false, false)
        }
    }

    fun release(stopPlayback: Boolean = false) {
        println("SimpleMediaServiceHandler.release called, cleanup")
        controller ?: return
        stopProgressUpdate()
        controllerFuture?.cancel(true)
        controllerFuture = null
        controller?.release()
        controller = null
        if (stopPlayback && appContext.isServiceRunning(SimpleMediaService::class.java)) {
            println("SimpleMediaServiceHandler.release stop service")
            appContext.stopService(Intent(appContext, SimpleMediaService::class.java))
        }
    }

    fun addMediaItem(mediaItem: MediaItem) {
        println("SimpleMediaServiceHandler.addMediaItem (controller connected: ${controller?.isConnected})")
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
    }

    fun addMediaItemList(mediaItemList: List<MediaItem>) {
        println("SimpleMediaServiceHandler.addMediaItemList (controller connected: ${controller?.isConnected})")
        controller?.setMediaItems(mediaItemList)
        controller?.prepare()
    }

    fun onPlayerEvent(playerEvent: PlayerEvent) {
        println("SimpleMediaServiceHandler.onPlayerEvent $playerEvent")
        val controller = this.controller ?: return
        when (playerEvent) {
            PlayerEvent.Backward -> controller.seekBack()
            PlayerEvent.Forward -> controller.seekForward()
            PlayerEvent.Stop -> stopProgressUpdate()
            PlayerEvent.PlayPause -> {
                if (controller.isPlaying) {
                    controller.pause()
                    stopProgressUpdate()
                } else {
                    controller.play()
                    _simpleMediaState.update {
                        SimpleMediaState.Playing(isPlaying = true)
                    }
                    startProgressUpdate()
                }
            }
            is PlayerEvent.UpdateProgress -> {
                controller.seekTo((controller.duration * playerEvent.newProgress).toLong())
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onPlaybackStateChanged(playbackState: Int) {
        println("SimpleMediaServiceHandler.onPlaybackStateChanged $playbackState")
        val controller = this.controller ?: return
        when (playbackState) {
            ExoPlayer.STATE_BUFFERING -> {
                _simpleMediaState.update {
                    SimpleMediaState.Buffering(controller.currentPosition)
                }
            }
            ExoPlayer.STATE_READY -> {
                _simpleMediaState.update {
                    SimpleMediaState.Ready(controller.duration)
                }
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        println("SimpleMediaServiceHandler.onIsPlayingChanged $isPlaying")
        _simpleMediaState.update { SimpleMediaState.Playing(isPlaying = isPlaying) }
        if (isPlaying) {
            startProgressUpdate()
        } else {
            stopProgressUpdate()
        }
    }

    private fun startProgressUpdate() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                delay(500)
                _simpleMediaState.update {
                    SimpleMediaState.Progress(controller?.currentPosition ?: 0)
                }
            }
        }
    }

    private fun stopProgressUpdate() {
        job?.cancel()
        job = null
        _simpleMediaState.update { SimpleMediaState.Playing(isPlaying = false) }
    }

    private fun Context.isServiceRunning(serviceClass: Class<out Service>) = try {
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    } catch (e: Exception) {
        false
    }
}

sealed class PlayerEvent {
    data object PlayPause : PlayerEvent()
    data object Backward : PlayerEvent()
    data object Forward : PlayerEvent()
    data object Stop : PlayerEvent()
    data class UpdateProgress(val newProgress: Float) : PlayerEvent()
}

sealed class SimpleMediaState {
    data object Initial : SimpleMediaState()
    data class Ready(val duration: Long) : SimpleMediaState()
    data class Progress(val progress: Long) : SimpleMediaState()
    data class Buffering(val progress: Long) : SimpleMediaState()
    data class Playing(val isPlaying: Boolean) : SimpleMediaState()
}