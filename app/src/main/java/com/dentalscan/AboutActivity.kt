package com.dentalscan

import android.os.Bundle
import android.widget.LinearLayout
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

        // Academic info (label / value rows; icons live in the layout)
        findViewById<TextView>(R.id.aboutAcademicTitle).text =
            if (isUrdu) "تعلیمی معلومات" else "Academic Information"

        findViewById<TextView>(R.id.aboutUniversityLabel).text =
            if (isUrdu) "یونیورسٹی" else "University"
        findViewById<TextView>(R.id.aboutUniversity).text =
            if (isUrdu) "شہید بے نظیر بھٹو ویمن یونیورسٹی، پشاور"
            else "Shaheed Benazir Bhutto Women University, Peshawar"

        findViewById<TextView>(R.id.aboutDepartmentLabel).text =
            if (isUrdu) "شعبہ" else "Department"
        findViewById<TextView>(R.id.aboutDepartment).text =
            if (isUrdu) "کمپیوٹر سائنس | BS کمپیوٹر سائنس"
            else "Computer Science | BS Computer Science"

        findViewById<TextView>(R.id.aboutSessionLabel).text =
            if (isUrdu) "سیشن" else "Session"
        findViewById<TextView>(R.id.aboutSession).text =
            if (isUrdu) "2022–2026 | گروپ 6"
            else "2022–2026 | Group 6"

        findViewById<TextView>(R.id.aboutSupervisorLabel).text =
            if (isUrdu) "نگران" else "Supervised by"
        findViewById<TextView>(R.id.aboutSupervisor).text = "Ma'am Naveen"

        // Developers
        findViewById<TextView>(R.id.aboutDevelopersTitle).text =
            if (isUrdu) "تیار کردہ" else "Developed By"
        findViewById<TextView>(R.id.aboutDeveloper1).text =
            "Khadija Nisar — Roll No. 05"
        findViewById<TextView>(R.id.aboutDeveloper2).text =
            "Aleena Khan — Roll No. 15"
        findViewById<TextView>(R.id.aboutDeveloper3).text =
            "Alishba — Roll No. 09"

        // App info
        findViewById<TextView>(R.id.aboutAppInfoTitle).text =
            if (isUrdu) "ایپ کی معلومات" else "App Information"
        findViewById<TextView>(R.id.aboutAppInfo).text =
            if (isUrdu) """
پلیٹ فارم: اینڈرائیڈ (کم از کم Android 7.1.1)
پروگرامنگ زبان: Kotlin
AI ماڈل: YOLOv8s (TensorFlow Lite)
ڈیٹا بیس: Room (SQLite)
زبانیں: اردو اور انگریزی
انٹرنیٹ: مکمل آف لائن — کوئی انٹرنیٹ درکار نہیں
مقصد: دانتوں کی صحت کے بارے میں آگاہی
            """.trimIndent() else """
Platform: Android (Minimum Android 7.1.1)
Programming Language: Kotlin
AI Model: YOLOv8s (TensorFlow Lite)
Database: Room (SQLite)
Languages: Urdu and English
Internet: Fully Offline — no internet required
Purpose: Dental health awareness and education
            """.trimIndent()

        // AI Model Details: a label / value table (English and Urdu text come from strings.xml)
        findViewById<TextView>(R.id.aboutModelTitle).text = getString(R.string.ai_model_details)
        val table = findViewById<LinearLayout>(R.id.aboutModelTable)
        table.removeAllViews()
        listOf(
            R.string.model_label_model to R.string.model_value_model,
            R.string.model_label_dataset to R.string.model_value_dataset,
            R.string.model_label_classes to R.string.model_value_classes,
            R.string.model_label_accuracy to R.string.model_value_accuracy,
            R.string.model_label_input to R.string.model_value_input,
            R.string.model_label_format to R.string.model_value_format,
            R.string.model_label_training to R.string.model_value_training
        ).forEach { (label, value) ->
            val row = layoutInflater.inflate(R.layout.item_model_row, table, false)
            row.findViewById<TextView>(R.id.rowLabel).setText(label)
            row.findViewById<TextView>(R.id.rowValue).setText(value)
            table.addView(row)
        }
        findViewById<TextView>(R.id.aboutModelInfo).text = getString(R.string.model_footnote)

        // Disclaimer note (warning icon lives in the layout)
        findViewById<TextView>(R.id.aboutDisclaimerNote).text =
            if (isUrdu)
                "یہ ایپ صرف تعلیمی مقاصد کے لیے ہے۔ یہ طبی تشخیص کا متبادل نہیں ہے۔ " +
                        "کسی بھی دانتوں کے مسئلے کے لیے مستند ڈینٹسٹ سے رجوع کریں۔"
            else
                "This app is for educational purposes only and does not constitute medical advice. " +
                        "Always consult a qualified dental professional for any dental concerns."
    }
}
