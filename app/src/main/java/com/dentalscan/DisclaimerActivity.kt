package com.dentalscan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.dentalscan.utils.LanguageManager

class DisclaimerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disclaimer)

        val isUrdu = LanguageManager.isUrdu(this)
        setupContent(isUrdu)
        setupListeners()
    }

    private fun setupContent(isUrdu: Boolean) {
        findViewById<TextView>(R.id.disclaimerTitle).text =
            if (isUrdu) "اہم اطلاع" else "Important Notice"

        findViewById<TextView>(R.id.disclaimerMainText).text =
            if (isUrdu)
                "DentalScan AI ایک تعلیمی ایپ ہے۔ یہ طبی آلہ نہیں ہے اور نہ ہی کسی لائسنس یافتہ دانتوں کے ڈاکٹر کی جگہ لے سکتی ہے۔ اس ایپ کے نتائج صرف آگاہی اور تعلیم کے لیے ہیں۔"
            else
                "DentalScan AI is an educational awareness application. It is not a medical device and " +
                        "cannot replace a licensed dental professional. The results provided by this app are " +
                        "for educational and awareness purposes only."

        findViewById<TextView>(R.id.disclaimerPointsTitle).text =
            if (isUrdu) "براہ کرم سمجھیں کہ:" else "Please understand that:"

        findViewById<TextView>(R.id.disclaimerPoints).text =
            if (isUrdu) """
- اس ایپ کے نتائج طبی تشخیص نہیں ہیں
- AI کی درستگی محدود ہو سکتی ہے اور ہر صورتحال میں مختلف ہو سکتی ہے
- دانتوں کے درد، خون آنے یا کسی بھی تکلیف پر فوری ڈینٹسٹ سے ملیں
- بچوں کی دانتوں کی صحت کے لیے باقاعدہ ڈینٹسٹ وزٹ ضروری ہے
- اس ایپ کے نتائج کو طبی علاج میں تاخیر کی وجہ نہ بنائیں
- آپ کی تصاویر صرف آپ کے آلے پر محفوظ ہوتی ہیں
            """.trimIndent() else """
- Results from this app are NOT a medical diagnosis
- AI accuracy may be limited and vary across different conditions and users
- For tooth pain, bleeding, or any discomfort — see a dentist immediately
- Regular professional dental check-ups are essential and cannot be replaced
- Do not delay seeking professional treatment based on this app's results
- Your images are stored only on your device — fully private and offline
            """.trimIndent()

        findViewById<TextView>(R.id.disclaimerRecommendation).text =
            if (isUrdu)
                "اس ایپ کو اپنی دانتوں کی صحت کے بارے میں آگاہی بڑھانے کے لیے استعمال کریں۔ " +
                        "کسی بھی تشویش کی صورت میں ہمیشہ ایک مستند دانتوں کے ڈاکٹر سے رجوع کریں۔ " +
                        "باقاعدگی سے ہر 6 ماہ بعد دانتوں کا معائنہ کروائیں۔"
            else
                "Use this app to build awareness about your dental health. Always consult a " +
                        "qualified dentist for any concerns. Schedule a professional dental check-up " +
                        "every 6 months for best oral health outcomes."

        val checkbox = findViewById<CheckBox>(R.id.checkboxAgree)
        checkbox.text =
            if (isUrdu) "میں سمجھتا/سمجھتی ہوں کہ یہ ایپ صرف تعلیمی مقاصد کے لیے ہے"
            else "I understand this app is for educational purposes only"

        findViewById<Button>(R.id.btnAgree).text =
            if (isUrdu) "میں متفق ہوں، جاری رکھیں" else "I Agree, Continue"
    }

    private fun setupListeners() {
        val checkbox = findViewById<CheckBox>(R.id.checkboxAgree)
        val btnAgree = findViewById<Button>(R.id.btnAgree)

        // Enable button only when checkbox is ticked
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            btnAgree.isEnabled = isChecked
            btnAgree.alpha = if (isChecked) 1.0f else 0.5f
        }

        btnAgree.setOnClickListener {
            // Mark disclaimer as accepted so it never shows again
            getSharedPreferences("settings", MODE_PRIVATE)
                .edit()
                .putBoolean("disclaimer_accepted", true)
                .apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}