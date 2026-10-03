package com.dentalscan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.content.res.ColorStateList
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.dentalscan.utils.HealthScore
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
    private lateinit var heroSubtitle: TextView
    private lateinit var footerDisclaimer: TextView

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
        heroSubtitle = findViewById(R.id.heroSubtitle)
        footerDisclaimer = findViewById(R.id.footerDisclaimer)
        // The whole hero card starts a scan, not just the button inside it
        findViewById<android.view.View>(R.id.heroCard).setOnClickListener { btnStartScan.performClick() }
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
            heroSubtitle.text = "5 زاویوں سے رہنمائی والا اسکین"
            footerDisclaimer.text = "صرف اسکریننگ کے لیے، طبی تشخیص نہیں"
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
            heroSubtitle.text = "5-angle guided scan"
            footerDisclaimer.text = "Screening aid only. Not a medical diagnosis."
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
        refreshHealthScoreBadge()
    }

    /** Small circle next to the profile name with the Dental Health Score (hidden before the first scan). */
    private fun refreshHealthScoreBadge() {
        val badge = findViewById<TextView>(R.id.healthScoreBadge)
        lifecycleScope.launch {
            val scans = HealthScore.scansFor(this@MainActivity)
            val score = HealthScore.compute(scans)
            if (score == null) {
                badge.visibility = View.GONE
                HealthScore.sync(this@MainActivity, null)
                return@launch
            }
            HealthScore.sync(this@MainActivity, score)

            val level = HealthScore.levelOf(score)
            badge.text = score.toString()
            badge.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this@MainActivity, level.colorRes))
            // Dark text on the orange level, white on red and green
            badge.setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (level == HealthScore.Level.FAIR) R.color.color_on_yellow else R.color.white
                )
            )
            badge.contentDescription =
                "${getString(R.string.health_score_title)}: $score, ${getString(level.labelRes)}"
            badge.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoiceFeedback.shutdown()
    }
}