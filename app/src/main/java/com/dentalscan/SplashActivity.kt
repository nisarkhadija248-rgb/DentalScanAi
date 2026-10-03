package com.dentalscan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_complete", false)
        val disclaimerAccepted = prefs.getBoolean("disclaimer_accepted", false)

        // Order: onboarding (first launch only) -> disclaimer (until accepted) -> home
        val next = when {
            !onboardingDone -> OnboardingActivity::class.java
            !disclaimerAccepted -> DisclaimerActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        finish()
    }
}