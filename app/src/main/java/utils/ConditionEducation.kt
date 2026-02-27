package com.dentalscan.utils

data class ConditionInfo(
    val what: String,
    val causes: String,
    val prevention: String
)

data class ActionPlan(
    val message: String,
    val urgency: String // "low", "medium", "high"
)

object ConditionEducation {

    fun getConditionInfo(condition: String, isUrdu: Boolean): ConditionInfo {
        return when (condition) {
            "Caries" -> if (isUrdu) ConditionInfo(
                what = "کیریز (دانتوں کی سڑن) دانتوں کی ایک عام بیماری ہے جس میں بیکٹیریا دانتوں کی سطح کو نقصان پہنچاتے ہیں۔ یہ چھوٹے چھیدوں سے شروع ہو کر بڑے درد کا باعث بن سکتی ہے۔",
                causes = "• میٹھی اور تیزابی غذائیں\n• دانتوں کی ناقص صفائی\n• رات کو میٹھا کھانا\n• منہ میں بیکٹیریا کا جمع ہونا",
                prevention = "• دن میں دو بار برش کریں\n• فلاس روزانہ استعمال کریں\n• میٹھی چیزیں کم کھائیں\n• ہر 6 ماہ بعد دانتوں کا معائنہ کروائیں"
            ) else ConditionInfo(
                what = "Dental caries (tooth decay) is one of the most common dental diseases. Bacteria in the mouth produce acids that attack tooth enamel, leading to cavities that can cause pain if left untreated.",
                causes = "• Sugary and acidic foods/drinks\n• Poor oral hygiene habits\n• Eating sweets before bedtime\n• Accumulation of bacteria (plaque)",
                prevention = "• Brush teeth twice daily with fluoride toothpaste\n• Floss daily\n• Reduce sugary food and drink intake\n• Visit your dentist every 6 months"
            )

            "Plaque" -> if (isUrdu) ConditionInfo(
                what = "پلاک دانتوں پر جمنے والی بیکٹیریا کی ایک پتلی، چپچپی تہہ ہے۔ اگر اسے صاف نہ کیا جائے تو یہ مسوڑھوں کی بیماری اور کیریز کا باعث بن سکتی ہے۔",
                causes = "• کھانے کے بعد برش نہ کرنا\n• میٹھے مشروبات کا زیادہ استعمال\n• فلاس نہ کرنا\n• منہ میں بیکٹیریا کا بڑھنا",
                prevention = "• کھانے کے بعد کلی کریں\n• دن میں دو بار برش کریں\n• اینٹی بیکٹیریل ماؤتھ واش استعمال کریں\n• فلاس روزانہ کریں"
            ) else ConditionInfo(
                what = "Plaque is a sticky, colorless film of bacteria that constantly forms on your teeth. If not removed regularly, plaque can harden into tartar and lead to gum disease and tooth decay.",
                causes = "• Not brushing after meals\n• High sugar and carbohydrate diet\n• Skipping flossing\n• Dry mouth conditions",
                prevention = "• Rinse mouth after every meal\n• Brush twice daily\n• Use antibacterial mouthwash\n• Floss daily to remove plaque between teeth"
            )

            "Calculus" -> if (isUrdu) ConditionInfo(
                what = "کیلکولس (ٹارٹر) پلاک کی سخت شکل ہے جو دانتوں پر جم جاتی ہے۔ اسے صرف دانتوں کا ڈاکٹر ہی صاف کر سکتا ہے۔ یہ مسوڑھوں کی سوزش کا سبب بنتی ہے۔",
                causes = "• پلاک کا طویل عرصے تک صاف نہ کرنا\n• تھوک میں معدنیات کا جمع ہونا\n• ناقص برشنگ تکنیک\n• سگریٹ نوشی",
                prevention = "• پلاک کو فوری صاف کریں\n• درست طریقے سے برش کریں\n• باقاعدگی سے دانتوں کی صفائی کروائیں\n• سگریٹ نوشی سے پرہیز کریں"
            ) else ConditionInfo(
                what = "Calculus (tartar) is hardened dental plaque that has mineralized on the tooth surface. Unlike plaque, it cannot be removed by brushing alone and requires professional dental cleaning.",
                causes = "• Plaque left unremoved for extended periods\n• Minerals in saliva hardening plaque\n• Improper brushing technique\n• Smoking",
                prevention = "• Remove plaque promptly through proper brushing\n• Use correct brushing technique\n• Get professional dental cleaning every 6 months\n• Avoid smoking"
            )

            else -> if (isUrdu) ConditionInfo(
                what = "دانتوں کی صحت بہت اہم ہے۔ باقاعدگی سے معائنہ کروائیں۔",
                causes = "• مختلف وجوہات",
                prevention = "• اچھی صفائی کی عادات اپنائیں"
            ) else ConditionInfo(
                what = "Maintaining good oral health is essential for overall wellbeing.",
                causes = "• Various factors",
                prevention = "• Maintain good oral hygiene habits"
            )
        }
    }

    fun getActionPlan(condition: String, severity: String, isUrdu: Boolean): ActionPlan {
        return when (severity) {
            "High" -> if (isUrdu) ActionPlan(
                message = "⚠️ فوری توجہ درکار ہے!\n\n• اگلے 2 ہفتوں میں ڈینٹسٹ سے ملیں\n• میٹھی اور تیزابی چیزوں سے پرہیز کریں\n• دن میں 3 بار برش کریں\n• درد یا حساسیت پر فوری ڈاکٹر سے رابطہ کریں",
                urgency = "high"
            ) else ActionPlan(
                message = "⚠️ Urgent Attention Required!\n\n• Visit a dentist within the next 2 weeks\n• Avoid sugary and acidic foods immediately\n• Brush 3 times daily until treated\n• Contact a dentist immediately if you experience pain or sensitivity",
                urgency = "high"
            )

            "Medium" -> if (isUrdu) ActionPlan(
                message = "⚡ درمیانی توجہ درکار ہے\n\n• اگلے 1-3 ماہ میں ڈینٹسٹ سے ملیں\n• فلاس کا روزانہ استعمال شروع کریں\n• ماؤتھ واش استعمال کریں\n• میٹھی چیزیں کم کریں",
                urgency = "medium"
            ) else ActionPlan(
                message = "⚡ Moderate Attention Needed\n\n• Schedule a dentist appointment within 1-3 months\n• Start flossing daily if not already doing so\n• Use an antibacterial mouthwash\n• Reduce sugar intake in your diet",
                urgency = "medium"
            )

            else -> if (isUrdu) ActionPlan(
                message = "✅ اچھی حالت — معمول برقرار رکھیں\n\n• دن میں دو بار 2 منٹ برش کریں\n• فلاس روزانہ کریں\n• ہر 6 ماہ بعد ڈینٹسٹ سے ملیں\n• پانی زیادہ پیئں",
                urgency = "low"
            ) else ActionPlan(
                message = "✅ Good Condition — Keep It Up!\n\n• Brush for 2 minutes twice daily\n• Floss daily\n• Visit dentist every 6 months for checkup\n• Drink plenty of water",
                urgency = "low"
            )
        }
    }
}