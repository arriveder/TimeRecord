package com.example.timerecord

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timerecord.adapter.LabelWithUsage
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RecordDetailActivity : AppCompatActivity() {

    private lateinit var recordId: String

    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var tvTime24: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tvTime12: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tvDate: androidx.appcompat.widget.AppCompatTextView
    private lateinit var cardNote: com.google.android.material.card.MaterialCardView
    private lateinit var tvNote: androidx.appcompat.widget.AppCompatTextView
    private lateinit var cardLabels: com.google.android.material.card.MaterialCardView
    private lateinit var chipGroupLabels: com.google.android.material.chip.ChipGroup
    private lateinit var tvNoLabels: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tvCreatedAt: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tvUpdatedAt: androidx.appcompat.widget.AppCompatTextView
    private lateinit var fabEdit: com.google.android.material.floatingactionbutton.FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_detail)

        recordId = intent.getStringExtra(EXTRA_RECORD_ID) ?: run {
            finish()
            return
        }

        bindViews()
        setupToolbar()
        setupViews()
        loadRecordData()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        tvTime24 = findViewById(R.id.tv_time_24)
        tvTime12 = findViewById(R.id.tv_time_12)
        tvDate = findViewById(R.id.tv_date)
        cardNote = findViewById(R.id.card_note)
        tvNote = findViewById(R.id.tv_note)
        cardLabels = findViewById(R.id.card_labels)
        chipGroupLabels = findViewById(R.id.chip_group_labels)
        tvNoLabels = findViewById(R.id.tv_no_labels)
        tvCreatedAt = findViewById(R.id.tv_created_at)
        tvUpdatedAt = findViewById(R.id.tv_updated_at)
        fabEdit = findViewById(R.id.fab_edit)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "记录详情"
    }

    private fun setupViews() {
        fabEdit.setOnClickListener {
            val intent = Intent(this, EditRecordActivity::class.java).apply {
                putExtra(EditRecordActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }
    }

    private fun loadRecordData() {
        lifecycleScope.launch {
            try {
                val viewModel = RecordViewModel(application)
                val recordWithLabels = viewModel.getRecordWithLabels(recordId)

                if (recordWithLabels == null) {
                    finish()
                    return@launch
                }

                val record = recordWithLabels.record
                val labels = recordWithLabels.labels

                // Time display
                tvTime24.text = record.time24
                tvTime12.text = "${record.time12} ${record.amPm}"
                tvDate.text = formatDateWithWeekday(record.date, record.timestamp)

                // Note
                if (record.note.isNullOrBlank()) {
                    cardNote.visibility = android.view.View.GONE
                } else {
                    cardNote.visibility = android.view.View.VISIBLE
                    tvNote.text = record.note
                }

                // Labels
                chipGroupLabels.removeAllViews()
                if (labels.isEmpty()) {
                    tvNoLabels.visibility = android.view.View.VISIBLE
                } else {
                    tvNoLabels.visibility = android.view.View.GONE
                    labels.forEach { label ->
                        val chip = Chip(this@RecordDetailActivity).apply {
                            text = label.name
                            isClickable = false

                            val bgColor = if (label.color != null) {
                                Color.parseColor(label.color)
                            } else {
                                Color.parseColor("#03A9F4")
                            }
                            chipBackgroundColor = ColorStateList.valueOf(bgColor)
                            setTextColor(getTextColorForBackground(bgColor))
                        }
                        chipGroupLabels.addView(chip)
                    }
                }

                // Metadata
                tvCreatedAt.text = "创建于 ${formatTimestamp(record.createdAt)}"
                tvUpdatedAt.text = "更新于 ${formatTimestamp(record.updatedAt)}"

            } catch (e: Exception) {
                finish()
            }
        }
    }

    private fun formatDateWithWeekday(dateStr: String, timestamp: Long): String {
        return try {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            val dateFormat = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())
            dateFormat.format(calendar.time)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        } catch (e: Exception) {
            "未知时间"
        }
    }

    private fun getTextColorForBackground(backgroundColor: Int): Int {
        val red = Color.red(backgroundColor)
        val green = Color.green(backgroundColor)
        val blue = Color.blue(backgroundColor)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000

        return if (brightness > 128) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "record_id"
    }
}
