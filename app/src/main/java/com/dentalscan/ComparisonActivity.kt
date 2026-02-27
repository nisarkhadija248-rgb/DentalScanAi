package com.dentalscan

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.dentalscan.data.AppDatabase
import com.dentalscan.data.ScanEntity
import com.dentalscan.utils.LanguageManager
import com.dentalscan.utils.ProfileManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ComparisonActivity : BaseActivity() {

    private lateinit var conditionSpinner: Spinner
    private lateinit var chart: LineChart
    private lateinit var beforeImage: ImageView
    private lateinit var afterImage: ImageView
    private lateinit var beforeDate: TextView
    private lateinit var afterDate: TextView
    private lateinit var improvementText: TextView
    private lateinit var titleText: TextView
    private lateinit var trendLabel: TextView
    private lateinit var comparisonLabel: TextView

    private var allScans: List<ScanEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comparison)

        initViews()
        setupLanguage()
        loadData()
    }

    private fun initViews() {
        conditionSpinner = findViewById(R.id.conditionSpinner)
        chart = findViewById(R.id.trendChart)
        beforeImage = findViewById(R.id.beforeImage)
        afterImage = findViewById(R.id.afterImage)
        beforeDate = findViewById(R.id.beforeDate)
        afterDate = findViewById(R.id.afterDate)
        improvementText = findViewById(R.id.improvementText)
        titleText = findViewById(R.id.titleText)
        trendLabel = findViewById(R.id.trendLabel)
        comparisonLabel = findViewById(R.id.comparisonLabel)
    }

    private fun setupLanguage() {
        val isUrdu = LanguageManager.isUrdu(this)

        titleText.text = if (isUrdu) "موازنہ" else "Compare Scans"
        trendLabel.text = if (isUrdu) "ترقی کا گراف" else "Progress Trend"
        comparisonLabel.text = if (isUrdu) "پہلے vs بعد" else "Before vs After"

        val conditions = if (isUrdu) {
            listOf("تمام حالات", "کیریز", "پلاک", "کیلکولس")
        } else {
            listOf("All Conditions", "Caries", "Plaque", "Calculus")
        }

        conditionSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            conditions
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        conditionSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?,
                    position: Int, id: Long
                ) {
                    updateComparison()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val database = AppDatabase.getDatabase(this@ComparisonActivity)
            val activeProfileId = ProfileManager.getActiveProfileId(this@ComparisonActivity)

            // Load scans for active profile only
            allScans = if (activeProfileId == -1L) {
                database.scanDao().getAllScans().first()
            } else {
                database.scanDao()
                    .getScansForUser(activeProfileId.toString())
                    .first()
            }

            withContext(Dispatchers.Main) {
                updateComparison()
            }
        }
    }

    private fun updateComparison() {
        val isUrdu = LanguageManager.isUrdu(this)

        if (allScans.size < 2) {
            improvementText.text = if (isUrdu)
                "موازنہ کے لیے کم از کم 2 اسکینز درکار ہیں"
            else
                "Need at least 2 scans for comparison"
            return
        }

        val conditionFilter = when (conditionSpinner.selectedItemPosition) {
            1 -> "Caries"
            2 -> "Plaque"
            3 -> "Calculus"
            else -> "All"
        }

        val filteredScans = (if (conditionFilter == "All") allScans
        else allScans.filter { it.condition == conditionFilter })
            .sortedBy { it.timestamp }

        if (filteredScans.size < 2) {
            improvementText.text = if (isUrdu)
                "اس حالت کے لیے کافی اسکینز نہیں"
            else
                "Not enough scans for this condition"
            return
        }

        val firstScan = filteredScans.first()
        val latestScan = filteredScans.last()

        // Load images
        beforeImage.setImageBitmap(BitmapFactory.decodeFile(firstScan.getThumbnailPath()))
        afterImage.setImageBitmap(BitmapFactory.decodeFile(latestScan.getThumbnailPath()))

        // Dates
        beforeDate.text = if (isUrdu)
            "پہلے: ${dateFormat.format(firstScan.timestamp)}"
        else
            "Before: ${dateFormat.format(firstScan.timestamp)}"

        afterDate.text = if (isUrdu)
            "بعد: ${dateFormat.format(latestScan.timestamp)}"
        else
            "After: ${dateFormat.format(latestScan.timestamp)}"

        // Improvement message
        val improvement = calculateImprovement(filteredScans)
        improvementText.text = when {
            improvement > 20 -> if (isUrdu)
                "✓ بہتری ہوئی! اچھی حفظان صحت جاری رکھیں۔"
            else "✓ Significant improvement! Keep up the good oral hygiene."
            improvement > 0 -> if (isUrdu)
                "→ تھوڑی بہتری دیکھی گئی۔"
            else "→ Slight improvement noted."
            improvement > -20 -> if (isUrdu)
                "→ حالت مستحکم ہے۔ موجودہ معمول برقرار رکھیں۔"
            else "→ Stable condition. Maintain current routine."
            else -> if (isUrdu)
                "⚠ حالت خراب ہو رہی ہے۔ براہ کرم ڈینٹسٹ سے رجوع کریں۔"
            else "⚠ Condition worsening. Please consult a dentist."
        }

        updateTrendChart(filteredScans)
    }

    private fun calculateImprovement(scans: List<ScanEntity>): Float {
        val earlySevere = scans.take(scans.size / 2).count { it.severity == "High" }
        val recentSevere = scans.takeLast(scans.size / 2).count { it.severity == "High" }
        return if (earlySevere == 0) 0f
        else ((earlySevere - recentSevere).toFloat() / earlySevere) * 100
    }

    private fun updateTrendChart(scans: List<ScanEntity>) {
        val entries = scans.mapIndexed { index, scan ->
            Entry(
                index.toFloat(), when (scan.severity) {
                    "High" -> 3f
                    "Medium" -> 2f
                    else -> 1f
                }
            )
        }

        val dataSet = LineDataSet(
            entries,
            if (LanguageManager.isUrdu(this)) "دانتوں کی صحت" else "Dental Health"
        ).apply {
            color = resources.getColor(android.R.color.holo_blue_dark, theme)
            setCircleColor(resources.getColor(android.R.color.holo_red_dark, theme))
            lineWidth = 2f
            circleRadius = 4f
        }

        chart.data = LineData(dataSet)
        chart.description.text =
            if (LanguageManager.isUrdu(this)) "کم بہتر ہے" else "Lower is better"
        chart.invalidate()
    }
}