package com.github.bnguyen527.snaptuney

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.SpotifyService

class MainActivity : AppCompatActivity() {
    private lateinit var _spotify: SpotifyService
    val spotify get() = _spotify

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intent.extras?.getString(WelcomeActivity.EXTRA_ACCESS_TOKEN)?.let { token ->
            Log.i(TAG, "Received access token")
            _spotify = SpotifyApi().setAccessToken(token).service
            Log.i(TAG, "Got SpotifyService object")
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}