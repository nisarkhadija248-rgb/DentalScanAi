package com.dentalscan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val disclaimerAccepted = prefs.getBoolean("disclaimer_accepted", false)

        if (disclaimerAccepted) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, DisclaimerActivity::class.java))
        }
        finish()
    }
}