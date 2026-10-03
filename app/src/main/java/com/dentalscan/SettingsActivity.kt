package com.dentalscan

import com.dentalscan.utils.ReminderScheduler
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.utils.HealthScore
import com.dentalscan.utils.VoiceFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {

    private lateinit var notificationSwitch: SwitchMaterial
    private lateinit var languageSwitch: SwitchMaterial
    private lateinit var statusText: TextView
    private lateinit var saveButton: Button
    private lateinit var notificationTimeText: TextView
    private lateinit var setNotificationButton: Button
    private lateinit var clearHistoryButton: Button
    private lateinit var themeSwitch: SwitchMaterial
    private lateinit var voiceSwitch: SwitchMaterial
    private lateinit var voiceTitle: TextView
    private lateinit var voiceDescription: TextView
    private lateinit var themeStatusText: TextView
    private lateinit var frequencyGroup: RadioGroup
    private lateinit var radioDaily: RadioButton
    private lateinit var radioEvery3Days: RadioButton
    private lateinit var radioWeekly: RadioButton
    private lateinit var radioSmart: RadioButton
    private lateinit var smartFrequencyText: TextView

    private var selectedHour: Int = 9
    private var selectedMinute: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initViews()
        setupListeners()
        loadSettings()
    }

    private fun initViews() {
        frequencyGroup = findViewById(R.id.frequencyGroup)
        radioDaily = findViewById(R.id.radioDaily)
        radioEvery3Days = findViewById(R.id.radioEvery3Days)
        radioWeekly = findViewById(R.id.radioWeekly)
        radioSmart = findViewById(R.id.radioSmart)
        smartFrequencyText = findViewById(R.id.smartFrequencyText)
        notificationSwitch = findViewById(R.id.notificationSwitch)
        languageSwitch = findViewById(R.id.languageSwitch)
        statusText = findViewById(R.id.statusText)
        saveButton = findViewById(R.id.saveButton)
        notificationTimeText = findViewById(R.id.notificationTimeText)
        setNotificationButton = findViewById(R.id.setNotificationButton)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)
        themeSwitch = findViewById(R.id.themeSwitch)
        themeStatusText = findViewById(R.id.themeStatusText)
        voiceSwitch = findViewById(R.id.voiceSwitch)
        voiceTitle = findViewById(R.id.voiceTitle)
        voiceDescription = findViewById(R.id.voiceDescription)
    }

    private fun loadSettings() {
        // Bug 1 fixed: prefs must be declared BEFORE it is used
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        val isUrdu = prefs.getBoolean("urdu_language", false)
        languageSwitch.isChecked = isUrdu
        updateLanguageStatus(isUrdu)

        selectedHour = prefs.getInt("notification_hour", 9)
        selectedMinute = prefs.getInt("notification_minute", 0)
        updateNotificationTimeText()

        val notifEnabled = prefs.getBoolean("notification_enabled", false)
        notificationSwitch.isChecked = notifEnabled

        voiceSwitch.isChecked = prefs.getBoolean("voice_enabled", true)
        val voiceStatus = findViewById<TextView>(R.id.voiceStatusText)
        fun showVoiceStatus(on: Boolean) {
            voiceStatus.text = getString(if (on) R.string.voice_status_on else R.string.voice_status_muted)
        }
        showVoiceStatus(voiceSwitch.isChecked)
        voiceSwitch.setOnCheckedChangeListener { _, checked -> showVoiceStatus(checked) }
        voiceTitle.text = if (isUrdu) "آواز کی رہنمائی" else "Voice Feedback"
        voiceDescription.text =
            if (isUrdu) "ہدایات اور نتائج بول کر سنائیں" else "Speak instructions and results aloud"

        val isDarkMode = prefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode
        updateThemeStatus(isDarkMode)

        // Frequency: Smart (follows the last scan) or one of the manual options
        radioSmart.setText(R.string.smart_option)
        radioDaily.setText(R.string.freq_daily)
        radioEvery3Days.setText(R.string.freq_every_3_days)
        radioWeekly.setText(R.string.freq_weekly)

        when (prefs.getInt("reminder_interval_days", 7)) {
            1 -> radioDaily.isChecked = true
            3 -> radioEvery3Days.isChecked = true
            else -> radioWeekly.isChecked = true
        }
        if (ReminderScheduler.mode(this) == ReminderScheduler.MODE_SMART) radioSmart.isChecked = true

        frequencyGroup.setOnCheckedChangeListener { _, _ -> refreshSmartText() }
        refreshSmartText()
    }

    /** With Smart selected, shows how often reminders currently come and why. */
    private fun refreshSmartText() {
        if (!radioSmart.isChecked) {
            smartFrequencyText.visibility = android.view.View.GONE
            return
        }
        smartFrequencyText.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            val scans = try {
                HealthScore.scansFor(this@SettingsActivity)
            } catch (e: Exception) {
                emptyList()
            }
            val hours = ReminderScheduler.smartHours(scans)
            val label = getString(ReminderScheduler.labelRes(hours))
            smartFrequencyText.text = getString(
                if (scans.isEmpty()) R.string.smart_current_none else R.string.smart_current_based,
                label
            )
        }
    }

    private fun setupListeners() {
        languageSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLanguageStatus(isChecked)
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateThemeStatus(isChecked)
        }

        setNotificationButton.setOnClickListener {
            showTimePicker()
        }

        clearHistoryButton.setOnClickListener {
            showClearHistoryDialog()
        }

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val editor = prefs.edit()

        val newUrduSetting = languageSwitch.isChecked
        val newDarkMode = themeSwitch.isChecked
        val notifEnabled = notificationSwitch.isChecked

        val smart = frequencyGroup.checkedRadioButtonId == R.id.radioSmart
        val intervalDays = when (frequencyGroup.checkedRadioButtonId) {
            R.id.radioDaily -> 1
            R.id.radioEvery3Days -> 3
            R.id.radioWeekly -> 7
            else -> prefs.getInt("reminder_interval_days", 7) // Smart: keep the last manual choice
        }

        editor.putString("reminder_mode", if (smart) ReminderScheduler.MODE_SMART else ReminderScheduler.MODE_MANUAL)
        editor.putBoolean("urdu_language", newUrduSetting)
        editor.putInt("notification_hour", selectedHour)
        editor.putInt("notification_minute", selectedMinute)
        editor.putBoolean("dark_mode", newDarkMode)
        editor.putBoolean("notification_enabled", notifEnabled)
        editor.putBoolean("voice_enabled", voiceSwitch.isChecked)
        editor.putInt("reminder_interval_days", intervalDays)
        editor.apply()

        if (!notifEnabled) {
            ReminderScheduler.cancel(this)
            finishSaving(newUrduSetting, newDarkMode)
        } else if (smart) {
            // Smart: the interval comes from the last scan, so look that up before leaving this screen
            lifecycleScope.launch {
                val scans = try {
                    HealthScore.scansFor(this@SettingsActivity)
                } catch (e: Exception) {
                    emptyList()
                }
                val hours = ReminderScheduler.smartHours(scans)
                ReminderScheduler.saveSmartHours(this@SettingsActivity, hours)
                ReminderScheduler.scheduleNext(this@SettingsActivity, selectedHour, selectedMinute, hours, afterScan = false)
                finishSaving(newUrduSetting, newDarkMode)
            }
        } else {
            ReminderScheduler.scheduleDaily(this, selectedHour, selectedMinute, intervalDays)
            finishSaving(newUrduSetting, newDarkMode)
        }
    }

    /** Applies voice, language and theme, tells the user, and returns to the home screen. */
    private fun finishSaving(newUrduSetting: Boolean, newDarkMode: Boolean) {
        VoiceFeedback.setEnabled(voiceSwitch.isChecked)
        VoiceFeedback.setLanguage(newUrduSetting)
        applyTheme(newDarkMode)

        val message = if (newUrduSetting) "ترتیبات محفوظ ہوگئیں" else "Settings saved"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        VoiceFeedback.speak(message)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun updateLanguageStatus(isUrdu: Boolean) {
        statusText.text = if (isUrdu) "Current: اردو (Urdu)" else "Current: English"
    }

    private fun updateThemeStatus(isDarkMode: Boolean) {
        themeStatusText.text = if (isDarkMode) "Dark Mode" else "Light Mode"
    }

    private fun updateNotificationTimeText() {
        val amPm = if (selectedHour < 12) "AM" else "PM"
        val displayHour = when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
        val timeString = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm)
        notificationTimeText.text = "Reminder at: $timeString"
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                updateNotificationTimeText()
            },
            selectedHour,
            selectedMinute,
            false
        ).show()
    }

    private fun showClearHistoryDialog() {
        val isUrdu = getSharedPreferences("settings", MODE_PRIVATE)
            .getBoolean("urdu_language", false)

        AlertDialog.Builder(this)
            .setTitle(if (isUrdu) "سابقہ ریکارڈ حذف کریں" else "Clear History")
            .setMessage(if (isUrdu) "کیا آپ واقعی تمام اسکینز حذف کرنا چاہتے ہیں؟" else "Are you sure you want to delete all scans?")
            .setPositiveButton(if (isUrdu) "ہاں" else "Yes") { _, _ -> clearAllHistory() }
            .setNegativeButton(if (isUrdu) "نہیں" else "No", null)
            .show()
    }

    private fun clearAllHistory() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = AppDatabase.getDatabase(this@SettingsActivity)
                database.scanDao().getAllScans().collect { scans ->
                    scans.forEach { scan ->
                        database.scanDao().deleteScan(scan)
                    }
                    withContext(Dispatchers.Main) {
                        val isUrdu = getSharedPreferences("settings", MODE_PRIVATE)
                            .getBoolean("urdu_language", false)
                        val message = if (isUrdu) "تمام ریکارڈز حذف کر دیے گئے" else "All history cleared"
                        Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
                        VoiceFeedback.speak(message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyTheme(isDarkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}