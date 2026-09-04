package com.example.muzo.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.muzo.MainActivity
import com.example.muzo.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MuziMediaSessionService : MediaSessionService() {

    companion object {
        const val TAG = "MuziMediaService"
        const val CHANNEL_ID = "androidx.media3.session.media_playback_channel_id"
        const val ACTION_TOGGLE_LIKE = "com.example.muzo.ACTION_TOGGLE_LIKE"

        private var instance: MuziMediaSessionService? = null

        var isSongLiked: Boolean = false
            set(value) {
                field = value
                instance?.updateCustomLayout()
            }

        var onLikeToggled: ((Boolean) -> Unit)? = null
        var onNextCallback: (() -> Unit)? = null
        var onPreviousCallback: (() -> Unit)? = null

        @Volatile
        private var _player: ExoPlayer? = null

        fun getPlayer(context: Context): ExoPlayer {
            val existing = _player
            if (existing != null) {
                return existing
            }
            return synchronized(MuziMediaSessionService::class.java) {
                if (_player == null) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build()

                    val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .setAllowCrossProtocolRedirects(true)
                        .setConnectTimeoutMs(15000)
                        .setReadTimeoutMs(15000)

                    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context.applicationContext)
                        .setDataSourceFactory(httpDataSourceFactory)

                    val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                            /* minBufferMs = */ 15_000,
                            /* maxBufferMs = */ 50_000,
                            /* bufferForPlaybackMs = */ 400,
                            /* bufferForPlaybackAfterRebufferMs = */ 1_000
                        )
                        .setPrioritizeTimeOverSizeThresholds(true)
                        .build()

                    _player = ExoPlayer.Builder(context.applicationContext)
                        .setMediaSourceFactory(mediaSourceFactory)
                        .setLoadControl(loadControl)
                        .setAudioAttributes(audioAttributes, true)
                        .setWakeMode(C.WAKE_MODE_NETWORK)
                        .setHandleAudioBecomingNoisy(true)
                        .build()
                }
                _player!!
            }
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, MuziMediaSessionService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MuziMediaSessionService: ${e.message}")
            }
        }
    }

    private var mediaSession: MediaSession? = null

    fun getLikeButton(): CommandButton {
        return CommandButton.Builder()
            .setDisplayName(if (isSongLiked) "Liked" else "Like")
            .setIconResId(if (isSongLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .build()
    }

    fun updateCustomLayout() {
        mediaSession?.let { session ->
            session.setCustomLayout(listOf(getLikeButton()))
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "MuziMediaSessionService onCreate")
        createNotificationChannel()

        val basePlayer = getPlayer(this)

        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {
            override fun seekToNext() {
                onNextCallback?.invoke() ?: super.seekToNext()
            }

            override fun seekToNextMediaItem() {
                onNextCallback?.invoke() ?: super.seekToNextMediaItem()
            }

            override fun seekToPrevious() {
                onPreviousCallback?.invoke() ?: super.seekToPrevious()
            }

            override fun seekToPreviousMediaItem() {
                onPreviousCallback?.invoke() ?: super.seekToPreviousMediaItem()
            }

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val sessionCallback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val availableSessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(availableSessionCommands)
                    .setCustomLayout(listOf(getLikeButton()))
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                if (customCommand.customAction == ACTION_TOGGLE_LIKE) {
                    val newLiked = !isSongLiked
                    isSongLiked = newLiked
                    onLikeToggled?.invoke(newLiked)
                    updateCustomLayout()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                return super.onCustomCommand(session, controller, customCommand, args)
            }
        }

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(pendingIntent)
            .setCallback(sessionCallback)
            .setCustomLayout(listOf(getLikeButton()))
            .build()

        addSession(mediaSession!!)

        try {
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build()
            notificationProvider.setSmallIcon(R.drawable.ic_music_note)
            setMediaNotificationProvider(notificationProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring DefaultMediaNotificationProvider: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Muzi Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback notification and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "MuziMediaSessionService onDestroy")
        mediaSession?.run {
            release()
            mediaSession = null
        }
        instance = null
        super.onDestroy()
    }
}
