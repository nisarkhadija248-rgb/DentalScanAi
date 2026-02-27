package com.dentalscan

import com.dentalscan.utils.ReminderScheduler
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.utils.VoiceFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {

    private lateinit var notificationSwitch: Switch
    private lateinit var languageSwitch: Switch
    private lateinit var statusText: TextView
    private lateinit var saveButton: Button
    private lateinit var notificationTimeText: TextView
    private lateinit var setNotificationButton: Button
    private lateinit var clearHistoryButton: Button
    private lateinit var themeSwitch: Switch
    private lateinit var themeStatusText: TextView
    private lateinit var frequencyGroup: RadioGroup
    private lateinit var radioDaily: RadioButton
    private lateinit var radioEvery3Days: RadioButton
    private lateinit var radioWeekly: RadioButton

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
        notificationSwitch = findViewById(R.id.notificationSwitch)
        languageSwitch = findViewById(R.id.languageSwitch)
        statusText = findViewById(R.id.statusText)
        saveButton = findViewById(R.id.saveButton)
        notificationTimeText = findViewById(R.id.notificationTimeText)
        setNotificationButton = findViewById(R.id.setNotificationButton)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)
        themeSwitch = findViewById(R.id.themeSwitch)
        themeStatusText = findViewById(R.id.themeStatusText)
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

        val isDarkMode = prefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode
        updateThemeStatus(isDarkMode)

        // Load frequency — must come after prefs is declared
        when (prefs.getInt("reminder_interval_days", 7)) {
            1 -> radioDaily.isChecked = true
            3 -> radioEvery3Days.isChecked = true
            else -> radioWeekly.isChecked = true
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

        val intervalDays = when (frequencyGroup.checkedRadioButtonId) {
            R.id.radioDaily -> 1
            R.id.radioEvery3Days -> 3
            else -> 7
        }

        editor.putBoolean("urdu_language", newUrduSetting)
        editor.putInt("notification_hour", selectedHour)
        editor.putInt("notification_minute", selectedMinute)
        editor.putBoolean("dark_mode", newDarkMode)
        editor.putBoolean("notification_enabled", notifEnabled)
        editor.putInt("reminder_interval_days", intervalDays)
        editor.apply()

        // Bug 2 fixed: only ONE scheduler call, with intervalDays passed correctly
        if (notifEnabled) {
            ReminderScheduler.scheduleDaily(this, selectedHour, selectedMinute, intervalDays)
        } else {
            ReminderScheduler.cancel(this)
        }

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