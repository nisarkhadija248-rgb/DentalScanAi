package com.dentalscan

import android.os.Bundle
import android.widget.TextView
import com.dentalscan.utils.LanguageManager
import java.util.Calendar

class TipsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tips)

        val isUrdu = LanguageManager.isUrdu(this)
        setupContent(isUrdu)
    }

    private fun setupContent(isUrdu: Boolean) {
        // Header
        findViewById<TextView>(R.id.tipsTitleText).text =
            if (isUrdu) "دانتوں کی صحت کے مشورے" else "Dental Health Tips"
        findViewById<TextView>(R.id.tipsSubtitleText).text =
            if (isUrdu) "اپنے دانتوں کو صحت مند رکھیں" else "Keep your teeth healthy every day"

        // Daily rotating tip
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val dailyTips = getDailyTips(isUrdu)
        val todayTip = dailyTips[dayOfYear % dailyTips.size]
        findViewById<TextView>(R.id.dailyTipText).text = todayTip

        // Section titles
        findViewById<TextView>(R.id.brushingTitle).text =
            if (isUrdu) "🪥 برش کرنا" else "🪥 Brushing"
        findViewById<TextView>(R.id.dietTitle).text =
            if (isUrdu) "🥗 خوراک" else "🥗 Diet"
        findViewById<TextView>(R.id.habitsTitle).text =
            if (isUrdu) "✅ اچھی عادات" else "✅ Healthy Habits"
        findViewById<TextView>(R.id.warningTitle).text =
            if (isUrdu) "⚠️ خطرے کی علامات" else "⚠️ Warning Signs"

        // Brushing tips
        findViewById<TextView>(R.id.brushingTips).text = if (isUrdu) """
- دن میں دو بار 2 منٹ تک برش کریں
- فلورائیڈ ٹوتھ پیسٹ استعمال کریں
- نرم برش استعمال کریں
- ہر 3 ماہ بعد برش بدلیں
- زبان بھی صاف کریں
- دائرے میں برش کریں، اوپر نیچے نہیں
        """.trimIndent() else """
- Brush for 2 minutes twice daily
- Use fluoride toothpaste
- Use a soft-bristled toothbrush
- Replace your toothbrush every 3 months
- Clean your tongue as well
- Use circular motions, not back and forth
        """.trimIndent()

        // Diet tips
        findViewById<TextView>(R.id.dietTips).text = if (isUrdu) """
- میٹھی اور تیزابی چیزیں کم کھائیں
- پانی زیادہ پیئں
- کچی سبزیاں کھائیں
- کھانے کے بعد چینی والی چیزیں نہ کھائیں
- دودھ اور دہی کا استعمال کریں (کیلشیم)
- سوڈا اور کولڈ ڈرنک سے پرہیز کریں
        """.trimIndent() else """
- Limit sugary and acidic foods and drinks
- Drink plenty of water throughout the day
- Eat crunchy vegetables like carrots and celery
- Avoid sweets right before bedtime
- Consume dairy products for calcium
- Avoid soda and carbonated drinks
        """.trimIndent()

        // Habits tips
        findViewById<TextView>(R.id.habitsTips).text = if (isUrdu) """
- روزانہ فلاس کریں
- ماؤتھ واش استعمال کریں
- ہر 6 ماہ بعد ڈینٹسٹ سے ملیں
- سگریٹ نوشی سے پرہیز کریں
- دانتوں سے بوتل نہ کھولیں
- رات سوتے وقت برش ضرور کریں
        """.trimIndent() else """
- Floss daily between all teeth
- Use mouthwash for extra protection
- Visit your dentist every 6 months
- Avoid smoking — it stains and damages teeth
- Never use teeth to open bottles or packages
- Always brush before bedtime
        """.trimIndent()

        // Warning signs
        findViewById<TextView>(R.id.warningTips).text = if (isUrdu) """
- دانتوں میں درد — فوری ڈینٹسٹ سے ملیں
- مسوڑھوں سے خون آنا
- دانتوں کی حساسیت (گرم/ٹھنڈے سے)
- منہ میں مسلسل بدبو
- دانتوں کا ہلنا
- مسوڑھوں کا سوجنا یا سرخ ہونا

ان علامات میں سے کوئی بھی ہو تو فوری ڈاکٹر سے رجوع کریں۔
        """.trimIndent() else """
- Toothache — see a dentist immediately
- Bleeding gums when brushing or flossing
- Tooth sensitivity to hot or cold
- Persistent bad breath
- Loose teeth
- Swollen or red gums

If you notice any of these signs, consult a dentist promptly.
        """.trimIndent()
    }

    private fun getDailyTips(isUrdu: Boolean): List<String> {
        return if (isUrdu) listOf(
            "آج کھانے کے بعد کلی کرنا نہ بھولیں!",
            "آج فلاس ضرور کریں — دانتوں کے درمیان صفائی بہت ضروری ہے۔",
            "پانی پینا دانتوں کو صاف رکھنے میں مدد کرتا ہے۔",
            "آج اپنا برش چیک کریں — کیا بدلنے کا وقت آ گیا ہے؟",
            "میٹھی چیزیں کم کریں — آپ کے دانت آپ کا شکریہ ادا کریں گے!",
            "آج رات سونے سے پہلے برش کرنا نہ بھولیں۔",
            "سبزیاں کھائیں — یہ دانتوں کو قدرتی طور پر صاف کرتی ہیں۔",
            "ڈینٹسٹ سے باقاعدہ ملتے رہیں — ہر 6 ماہ بعد!",
            "فلورائیڈ ٹوتھ پیسٹ استعمال کریں۔",
            "آج 2 منٹ تک برش کریں — ٹائمر لگائیں!"
        ) else listOf(
            "Don't forget to rinse after every meal today!",
            "Floss today — cleaning between teeth prevents 40% of decay!",
            "Drinking water helps wash away food and bacteria.",
            "Check your toothbrush today — is it time for a new one?",
            "Cut back on sugar today — your teeth will thank you!",
            "Don't forget to brush before bed tonight.",
            "Eat crunchy vegetables — they naturally clean your teeth!",
            "Schedule your dentist visit if it's been over 6 months.",
            "Use fluoride toothpaste for stronger enamel.",
            "Set a timer and brush for a full 2 minutes today!"
        )
    }
}