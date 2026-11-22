package com.homecinema.android

import android.app.Application
import android.util.Log
import com.homecinema.core.appContext
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class HomeCinemaApp : Application() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        appContext = this
        YoutubeDL.getInstance().init(this)
        FFmpeg.getInstance().init(this)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(this@HomeCinemaApp)
                Log.i("HomeCinemaApp", "yt-dlp update: $status")
            } catch (e: Exception) {
                Log.w("HomeCinemaApp", "yt-dlp update failed: ${e.message}")
            }
            Log.i("HomeCinemaApp", "yt-dlp version: ${YoutubeDL.getInstance().versionName(this@HomeCinemaApp)}")
        }
    }
}
