package com.dentalscan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.VoiceFeedback
import com.dentalscan.utils.ProfileManager

class MainActivity : BaseActivity() {
    private lateinit var activeProfileBadge: TextView
    private lateinit var btnProfiles: Button
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var btnStartScan: Button
    private lateinit var btnHistory: Button
    private lateinit var btnCompare: Button
    private lateinit var btnSettings: Button
    private lateinit var btnTips: Button
    private lateinit var btnAbout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        updateText()
        setupListeners()

        VoiceFeedback.initialize(this)
        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 200
                )
            }
        }
    }

    private fun initViews() {
        activeProfileBadge = findViewById(R.id.activeProfileBadge)
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        btnStartScan = findViewById(R.id.btnStartScan)
        btnHistory = findViewById(R.id.btnHistory)
        btnCompare = findViewById(R.id.btnCompare)
        btnSettings = findViewById(R.id.btnSettings)
        btnProfiles = findViewById(R.id.btnProfiles)
        btnTips = findViewById(R.id.btnTips)
        btnAbout = findViewById(R.id.btnAbout)
    }

    private fun updateText() {
        val isUrdu = LanguageManager.isUrdu(this)
        val activeName = ProfileManager.getActiveProfileName(this)
        if (isUrdu) {
            titleText.text = "ڈینٹل اسکین AI"
            subtitleText.text = "ذہین دانتوں کی صحت کی نگرانی"
            btnStartScan.text = "نیا اسکین شروع کریں"
            btnHistory.text = "سابقہ ریکارڈ دیکھیں"
            btnCompare.text = "اسکینز کا موازنہ کریں"
            btnSettings.text = "ترتیبات"
            btnProfiles.text = "پروفائلز"
            activeProfileBadge.text = "فعال: $activeName"
            btnTips.text = "دانتوں کے مشورے"
            btnAbout.text = "ایپ کے بارے میں"
        } else {
            titleText.text = "DentalScan AI"
            subtitleText.text = "Smart Dental Health Monitoring"
            btnStartScan.text = "Start New Scan"
            btnHistory.text = "View History"
            btnCompare.text = "Compare Scans"
            btnSettings.text = "Settings"
            btnProfiles.text = "Profiles"
            activeProfileBadge.text = "Active: $activeName"
            btnTips.text = "Dental Tips"
            btnAbout.text = "About"
        }
    }

    private fun setupListeners() {
        btnProfiles.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        btnStartScan.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        btnCompare.setOnClickListener {
            startActivity(Intent(this, ComparisonActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        btnTips.setOnClickListener {
            startActivity(Intent(this, TipsActivity::class.java))
        }
        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateText() // Refresh text when returning from Settings
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceFeedback.shutdown()
    }
}