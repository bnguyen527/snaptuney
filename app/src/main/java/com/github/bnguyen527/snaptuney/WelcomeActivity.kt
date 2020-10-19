package com.github.bnguyen527.snaptuney

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.bnguyen527.snaptuney.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            loginButton.setOnClickListener {
                loginButton.visibility = View.GONE
                loggedInTextView.visibility = View.VISIBLE
                startMakingButton.visibility = View.VISIBLE
            }
            startMakingButton.setOnClickListener {
                startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
            }
        }
    }
}