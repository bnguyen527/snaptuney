package com.github.bnguyen527.snaptuney

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (intent.extras?.getString(WelcomeActivity.EXTRA_ACCESS_TOKEN) != null) {
            Log.i(TAG, "Received access token")
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}