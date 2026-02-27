package com.dentalscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dentalscan.adapter.ScanHistoryAdapter
import com.dentalscan.data.AppDatabase
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ProfileManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HistoryActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private lateinit var titleText: TextView
    private lateinit var progressFeedbackText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        // All findViewByIds MUST come after setContentView
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.recyclerView)
        emptyText = findViewById(R.id.emptyText)
        progressFeedbackText = findViewById(R.id.progressFeedbackText)

        val isUrdu = LanguageManager.isUrdu(this)
        titleText.text = if (isUrdu) "سابقہ ریکارڈ" else "Scan History"

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(this@HistoryActivity)
            val activeProfileId = ProfileManager.getActiveProfileId(this@HistoryActivity)
            val isUrdu = LanguageManager.isUrdu(this@HistoryActivity)

            val scans = if (activeProfileId == -1L) {
                database.scanDao().getAllScans().first()
            } else {
                database.scanDao()
                    .getScansForUser(activeProfileId.toString())
                    .first()
            }

            if (scans.isEmpty()) {
                emptyText.text = if (isUrdu) "کوئی ریکارڈ نہیں ملا" else "No scans yet"
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
                progressFeedbackText.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                val adapter = ScanHistoryAdapter { scan ->
                    val intent = Intent(this@HistoryActivity, AnalysisActivity::class.java)
                    intent.putExtra("image_path", scan.imagePath)
                    intent.putExtra("view_only", true)
                    intent.putExtra("condition", scan.condition)
                    intent.putExtra("confidence", scan.confidence)
                    intent.putExtra("severity", scan.severity)
                    startActivity(intent)
                }
                adapter.submitList(scans)
                recyclerView.adapter = adapter
                showProgressFeedback(scans)
            }
        }
    }

    private fun showProgressFeedback(scans: List<com.dentalscan.data.ScanEntity>) {
        val isUrdu = LanguageManager.isUrdu(this)

        if (scans.size < 2) {
            progressFeedbackText.visibility = View.GONE
            return
        }

        val firstHalf = scans.take(scans.size / 2)
        val secondHalf = scans.takeLast(scans.size / 2)

        val firstHighCount = firstHalf.count { it.severity == "High" }
        val secondHighCount = secondHalf.count { it.severity == "High" }

        val message = when {
            secondHighCount < firstHighCount -> if (isUrdu)
                "🎉 شاباش! آپ کے دانتوں کی صحت بہتر ہو رہی ہے!"
            else
                "🎉 Great progress! Your dental health is improving!"

            secondHighCount == firstHighCount && firstHighCount == 0 -> if (isUrdu)
                "✅ بہترین! آپ کے دانت مسلسل صحت مند ہیں!"
            else
                "✅ Excellent! Your teeth have been consistently healthy!"

            secondHighCount == firstHighCount -> if (isUrdu)
                "→ آپ کی حالت مستحکم ہے۔ معمول برقرار رکھیں۔"
            else
                "→ Your condition is stable. Keep up your routine."

            else -> if (isUrdu)
                "⚠️ حالت خراب ہو رہی ہے۔ براہ کرم ڈینٹسٹ سے ملیں۔"
            else
                "⚠️ Condition worsening. Please consult a dentist soon."
        }

        progressFeedbackText.text = message
        progressFeedbackText.visibility = View.VISIBLE

        val color = when {
            secondHighCount < firstHighCount ->
                resources.getColor(R.color.healthy_green, theme)
            secondHighCount > firstHighCount ->
                resources.getColor(R.color.caries_red, theme)
            else ->
                resources.getColor(R.color.primary, theme)
        }
        progressFeedbackText.setBackgroundColor(color)
    }
}