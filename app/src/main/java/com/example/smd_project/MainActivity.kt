package com.example.smd_project

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logo = findViewById<ImageView>(R.id.appLogo)

        val zoomAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        logo.startAnimation(zoomAnim)

        mediaPlayer = MediaPlayer.create(this, R.raw.logosound)
        mediaPlayer?.start()

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@MainActivity, ChooseLoginActivity::class.java))
            finish()
        }, 3500)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
