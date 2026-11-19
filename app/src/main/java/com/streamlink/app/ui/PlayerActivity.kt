package com.streamlink.app.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var bufferingSpinner: ProgressBar
    private lateinit var titleView: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full screen immersive landscape
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        )

        // Build layout: PlayerView + buffering spinner + title
        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF000000.toInt())

        playerView = PlayerView(this).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
        root.addView(playerView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Buffering spinner overlay
        bufferingSpinner = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            indeterminateTintList = android.content.res.ColorStateList.valueOf(0xFFE50914.toInt())
            visibility = View.GONE
        }
        val spinnerParams = FrameLayout.LayoutParams(120, 120).apply {
            gravity = Gravity.CENTER
        }
        root.addView(bufferingSpinner, spinnerParams)

        // Title overlay
        titleView = TextView(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(40, 60, 40, 0)
        }
        val titleParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.TOP }
        root.addView(titleView, titleParams)

        setContentView(root)

        val path  = intent.getStringExtra("PATH")  ?: return
        val title = intent.getStringExtra("TITLE") ?: ""
        val position = intent.getLongExtra("POSITION", 0L)

        titleView.text = title
        initPlayer(path, title, position)
    }

    private fun initPlayer(path: String, title: String, startPosition: Long) {
        player = ExoPlayer.Builder(this).build().also { exo ->
            playerView.player = exo

            val uri = when {
                path.startsWith("http") -> Uri.parse(path)
                path.startsWith("file://") -> Uri.parse(path)
                else -> Uri.fromFile(java.io.File(path))
            }

            exo.setMediaItem(
                MediaItem.Builder()
                    .setUri(uri)
                    .setMediaId(title)
                    .build()
            )

            if (startPosition > 0) exo.seekTo(startPosition)

            exo.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> bufferingSpinner.visibility = View.VISIBLE
                        Player.STATE_READY     -> bufferingSpinner.visibility = View.GONE
                        Player.STATE_ENDED     -> finish()
                        Player.STATE_IDLE      -> {}
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    // אם הקובץ עדיין מוריד ויש שגיאה — נסה שוב אחרי 3 שניות
                    handler.postDelayed({
                        exo.prepare()
                        exo.play()
                    }, 3000)
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
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        super.onDestroy()
    }
}
