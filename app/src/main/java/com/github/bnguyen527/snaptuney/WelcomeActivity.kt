package com.github.bnguyen527.snaptuney

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.bnguyen527.snaptuney.databinding.ActivityWelcomeBinding
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class WelcomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            loginButton.setOnClickListener {
                val request = AuthorizationRequest.Builder(
                    CLIENT_ID,
                    AuthorizationResponse.Type.TOKEN,
                    REDIRECT_URI
                ).setScopes(AUTH_SCOPES.toTypedArray()).build()
                AuthorizationClient.openLoginActivity(this@WelcomeActivity, REQUEST_CODE, request)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    binding.apply {
                        loginButton.visibility = View.GONE
                        loggedInTextView.visibility = View.VISIBLE
                        startMakingButton.setOnClickListener {
                            startActivity(
                                Intent(this@WelcomeActivity, MainActivity::class.java).putExtra(
                                    EXTRA_ACCESS_TOKEN,
                                    response.accessToken
                                )
                            )
                        }
                        startMakingButton.visibility = View.VISIBLE
                    }
                    Toast.makeText(
                        this,
                        getString(R.string.login_success_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                AuthorizationResponse.Type.ERROR ->
                    Toast.makeText(
                        this,
                        getString(R.string.login_failed_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                else ->
                    Toast.makeText(
                        this,
                        getString(R.string.login_canceled_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
            }
        }
    }

    companion object {
        const val EXTRA_ACCESS_TOKEN = "com.github.bnguyen527.snaptuney.EXTRA_ACCESS_TOKEN"
        private const val CLIENT_ID = "cceb1c861c3a4cbfb6de3c67dfc32179"
        private const val REDIRECT_URI = "com.github.bnguyen527.snaptuney://callback"
        private val AUTH_SCOPES = listOf("playlist-read-private", "playlist-read-collaborative")
        private const val REQUEST_CODE = 1337
    }
}