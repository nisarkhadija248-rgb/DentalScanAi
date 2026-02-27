package com.dentalscan

import android.os.Bundle
import android.widget.TextView
import com.dentalscan.utils.LanguageManager

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val isUrdu = LanguageManager.isUrdu(this)
        setupContent(isUrdu)
    }

    private fun setupContent(isUrdu: Boolean) {
        // Header
        findViewById<TextView>(R.id.aboutAppName).text = "DentalScan AI"
        findViewById<TextView>(R.id.aboutVersion).text =
            if (isUrdu) "ورژن 1.0.0" else "Version 1.0.0"
        findViewById<TextView>(R.id.aboutTagline).text =
            if (isUrdu) "ذہین دانتوں کی صحت کی نگرانی" else "Smart Dental Health Monitoring"

        // Academic info
        findViewById<TextView>(R.id.aboutAcademicTitle).text =
            if (isUrdu) "🎓 تعلیمی معلومات" else "🎓 Academic Information"
        findViewById<TextView>(R.id.aboutUniversity).text =
            if (isUrdu) "🏛️ یونیورسٹی: شہید بے نظیر بھٹو ویمن یونیورسٹی، پشاور"
            else "🏛️ University: Shaheed Benazir Bhutto Women University, Peshawar"
        findViewById<TextView>(R.id.aboutDepartment).text =
            if (isUrdu) "📚 شعبہ: کمپیوٹر سائنس | BS کمپیوٹر سائنس"
            else "📚 Department: Computer Science | BS Computer Science"
        findViewById<TextView>(R.id.aboutSession).text =
            if (isUrdu) "📅 سیشن: 2022–2026 | گروپ 6"
            else "📅 Session: 2022–2026 | Group 6"
        findViewById<TextView>(R.id.aboutSupervisor).text =
            if (isUrdu) "👩‍🏫 نگران: Ma'am Naveen"
            else "👩‍🏫 Supervised by: Ma'am Naveen"

        // Developers
        findViewById<TextView>(R.id.aboutDevelopersTitle).text =
            if (isUrdu) "👩‍💻 تیار کردہ" else "👩‍💻 Developed By"
        findViewById<TextView>(R.id.aboutDeveloper1).text =
            "• Khadija Nisar — Roll No. 05"
        findViewById<TextView>(R.id.aboutDeveloper2).text =
            "• Aleena Khan — Roll No. 15"
        findViewById<TextView>(R.id.aboutDeveloper3).text =
            "• Alishba — Roll No. 09"

        // App info
        findViewById<TextView>(R.id.aboutAppInfoTitle).text =
            if (isUrdu) "📱 ایپ کی معلومات" else "📱 App Information"
        findViewById<TextView>(R.id.aboutAppInfo).text =
            if (isUrdu) """
پلیٹ فارم: اینڈرائیڈ (کم از کم Android 7.1.1)
پروگرامنگ زبان: Kotlin
AI ماڈل: YOLOv8n (TensorFlow Lite)
ڈیٹا بیس: Room (SQLite)
زبانیں: اردو اور انگریزی
انٹرنیٹ: مکمل آف لائن — کوئی انٹرنیٹ درکار نہیں
مقصد: دانتوں کی صحت کے بارے میں آگاہی
            """.trimIndent() else """
Platform: Android (Minimum Android 7.1.1)
Programming Language: Kotlin
AI Model: YOLOv8n (TensorFlow Lite)
Database: Room (SQLite)
Languages: Urdu and English
Internet: Fully Offline — no internet required
Purpose: Dental health awareness and education
            """.trimIndent()

        // Disclaimer note
        findViewById<TextView>(R.id.aboutDisclaimerNote).text =
            if (isUrdu)
                "⚠️ یہ ایپ صرف تعلیمی مقاصد کے لیے ہے۔ یہ طبی تشخیص کا متبادل نہیں ہے۔ " +
                        "کسی بھی دانتوں کے مسئلے کے لیے مستند ڈینٹسٹ سے رجوع کریں۔"
            else
                "⚠️ This app is for educational purposes only and does not constitute medical advice. " +
                        "Always consult a qualified dental professional for any dental concerns."
    }
}