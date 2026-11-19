package com.streamlink.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen immersive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        playerView = PlayerView(this)
        setContentView(playerView)

        val path  = intent.getStringExtra("PATH")  ?: return
        val title = intent.getStringExtra("TITLE") ?: ""

        initPlayer(path, title)
    }

    private fun initPlayer(path: String, title: String) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo

            val uri = when {
                path.startsWith("http") -> Uri.parse(path)
                else -> Uri.fromFile(java.io.File(path))
            }

            exo.setMediaItem(MediaItem.Builder()
                .setUri(uri)
                .setMediaId(title)
                .build())

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) finish()
                }
            })

            exo.prepare()
            exo.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
    }

    override fun onResume() {
        super.onResume()
        player?.play()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
