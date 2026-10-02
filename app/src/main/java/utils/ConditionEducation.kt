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
        // Case-insensitive: the model reports "Gingivitis", older saved scans may say "gingivitis"
        return when (condition.lowercase()) {
            "caries" -> if (isUrdu) ConditionInfo(
                what = "کیریز (دانتوں کی سڑن) دانتوں کی ایک عام بیماری ہے جس میں بیکٹیریا دانتوں کی سطح کو نقصان پہنچاتے ہیں۔ یہ چھوٹے چھیدوں سے شروع ہو کر بڑے درد کا باعث بن سکتی ہے۔",
                causes = "• میٹھی اور تیزابی غذائیں\n• دانتوں کی ناقص صفائی\n• رات کو میٹھا کھانا\n• منہ میں بیکٹیریا کا جمع ہونا",
                prevention = "• دن میں دو بار برش کریں\n• فلاس روزانہ استعمال کریں\n• میٹھی چیزیں کم کھائیں\n• ہر 6 ماہ بعد دانتوں کا معائنہ کروائیں"
            ) else ConditionInfo(
                what = "Dental caries (tooth decay) is one of the most common dental diseases. Bacteria in the mouth produce acids that attack tooth enamel, leading to cavities that can cause pain if left untreated.",
                causes = "• Sugary and acidic foods/drinks\n• Poor oral hygiene habits\n• Eating sweets before bedtime\n• Accumulation of bacteria (plaque)",
                prevention = "• Brush teeth twice daily with fluoride toothpaste\n• Floss daily\n• Reduce sugary food and drink intake\n• Visit your dentist every 6 months"
            )

            "gingivitis" -> if (isUrdu) ConditionInfo(
                what = "مسوڑوں کی سوزش (جنجیوائٹس) مسوڑھوں کی وہ سوجن ہے جو دانتوں پر جمنے والی پلاک (بیکٹیریا کی چپچپی تہہ) کی وجہ سے ہوتی ہے۔ اس میں مسوڑھے سرخ، سوجے ہوئے اور خون آلود ہو سکتے ہیں۔ بروقت صفائی اور علاج سے یہ ٹھیک ہو سکتی ہے۔",
                causes = "• دانتوں اور مسوڑھوں کی لکیر پر پلاک کا جمع ہونا\n• برش اور فلاس میں کمی\n• سگریٹ نوشی یا تمباکو کا استعمال\n• ہارمونز میں تبدیلی، بعض ادویات یا ذیابیطس",
                prevention = "• دن میں دو بار فلورائیڈ ٹوتھ پیسٹ سے برش کریں\n• روزانہ فلاس کریں\n• اینٹی بیکٹیریل ماؤتھ واش استعمال کریں\n• باقاعدگی سے دانتوں کی صفائی کروائیں"
            ) else ConditionInfo(
                what = "Gingivitis is inflammation of the gums caused by plaque, a sticky film of bacteria that builds up along the gumline. Signs include red, swollen, or bleeding gums. It is the earliest stage of gum disease and is reversible with good oral care, but can progress if ignored.",
                causes = "• Plaque left on the teeth and gumline\n• Poor brushing and flossing habits\n• Smoking or tobacco use\n• Hormonal changes, some medications, or diabetes",
                prevention = "• Brush twice daily with fluoride toothpaste\n• Floss daily to remove plaque between teeth\n• Use an antibacterial mouthwash\n• Visit your dentist for regular cleanings"
            )

            "calculus" -> if (isUrdu) ConditionInfo(
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
                message = "فوری توجہ درکار ہے!\n\n• اگلے 2 ہفتوں میں ڈینٹسٹ سے ملیں\n• میٹھی اور تیزابی چیزوں سے پرہیز کریں\n• دن میں 3 بار برش کریں\n• درد یا حساسیت پر فوری ڈاکٹر سے رابطہ کریں",
                urgency = "high"
            ) else ActionPlan(
                message = "Urgent Attention Required!\n\n• Visit a dentist within the next 2 weeks\n• Avoid sugary and acidic foods immediately\n• Brush 3 times daily until treated\n• Contact a dentist immediately if you experience pain or sensitivity",
                urgency = "high"
            )

            "Medium" -> if (isUrdu) ActionPlan(
                message = "درمیانی توجہ درکار ہے\n\n• اگلے 1-3 ماہ میں ڈینٹسٹ سے ملیں\n• فلاس کا روزانہ استعمال شروع کریں\n• ماؤتھ واش استعمال کریں\n• میٹھی چیزیں کم کریں",
                urgency = "medium"
            ) else ActionPlan(
                message = "Moderate Attention Needed\n\n• Schedule a dentist appointment within 1-3 months\n• Start flossing daily if not already doing so\n• Use an antibacterial mouthwash\n• Reduce sugar intake in your diet",
                urgency = "medium"
            )

            else -> if (isUrdu) ActionPlan(
                message = "اچھی حالت — معمول برقرار رکھیں\n\n• دن میں دو بار 2 منٹ برش کریں\n• فلاس روزانہ کریں\n• ہر 6 ماہ بعد ڈینٹسٹ سے ملیں\n• پانی زیادہ پیئں",
                urgency = "low"
            ) else ActionPlan(
                message = "Good Condition — Keep It Up!\n\n• Brush for 2 minutes twice daily\n• Floss daily\n• Visit dentist every 6 months for checkup\n• Drink plenty of water",
                urgency = "low"
            )
        }
    }
}
