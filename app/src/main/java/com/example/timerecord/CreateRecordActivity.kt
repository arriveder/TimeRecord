package com.example.timerecord

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timerecord.entity.Label
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateRecordActivity : AppCompatActivity() {

    private val viewModel: RecordViewModel by viewModels()
    private val selectedLabelIds = mutableSetOf<String>()
    private val availableLabels = mutableListOf<Label>()
    private var updateTimeJob: Job? = null

    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar
    private lateinit var tvCurrentTime: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tvCurrentDate: androidx.appcompat.widget.AppCompatTextView
    private lateinit var tilNote: com.google.android.material.textfield.TextInputLayout
    private lateinit var etNote: com.google.android.material.textfield.TextInputEditText
    private lateinit var tvLabelsTitle: androidx.appcompat.widget.AppCompatTextView
    private lateinit var chipGroupLabels: com.google.android.material.chip.ChipGroup
    private lateinit var tilNewLabel: com.google.android.material.textfield.TextInputLayout
    private lateinit var etNewLabel: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnAddLabel: com.google.android.material.button.MaterialButton
    private lateinit var btnSave: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_record)

        bindViews()
        setupToolbar()
        setupViews()
        observeViewModel()
        startUpdateTime()
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        tvCurrentTime = findViewById(R.id.tv_current_time)
        tvCurrentDate = findViewById(R.id.tv_current_date)
        tilNote = findViewById(R.id.til_note)
        etNote = findViewById(R.id.et_note)
        tvLabelsTitle = findViewById(R.id.tv_labels_title)
        chipGroupLabels = findViewById(R.id.chip_group_labels)
        tilNewLabel = findViewById(R.id.til_new_label)
        etNewLabel = findViewById(R.id.et_new_label)
        btnAddLabel = findViewById(R.id.btn_add_label)
        btnSave = findViewById(R.id.btn_save)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "创建记录"
    }

    private fun setupViews() {
        btnSave.setOnClickListener {
            saveRecord()
        }

        btnAddLabel.setOnClickListener {
            addNewLabel()
        }

        etNewLabel.setOnEditorActionListener { _, _, _ ->
            addNewLabel()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.userLabels.observe(this) { labels ->
            availableLabels.clear()
            availableLabels.addAll(labels)
            updateLabelChips()
        }

        viewModel.createLabelResult.observe(this) { result ->
            result.onSuccess { label ->
                Snackbar.make(
                    btnSave,
                    "标签 \"${label.name}\" 添加成功",
                    Snackbar.LENGTH_SHORT
                ).show()
                etNewLabel.text?.clear()
            }.onFailure { error ->
                val message = when (error.message) {
                    "Label already exists" -> "标签已存在"
                    else -> "添加标签失败: ${error.message}"
                }
                Snackbar.make(btnSave, message, Snackbar.LENGTH_SHORT).show()
            }
        }

        viewModel.saveResult.observe(this) { result ->
            result.onSuccess {
                Snackbar.make(
                    btnSave,
                    "记录保存成功",
                    Snackbar.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure { error ->
                Snackbar.make(
                    btnSave,
                    "保存失败: ${error.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

        viewModel.loadLabels()
    }

    private fun startUpdateTime() {
        updateTimeJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                updateDateTime()
                delay(1000)
            }
        }
    }

    private fun updateDateTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd EEEE", Locale.getDefault())

        tvCurrentTime.text = timeFormat.format(calendar.time)
        tvCurrentDate.text = dateFormat.format(calendar.time)
    }

    private fun updateLabelChips() {
        chipGroupLabels.removeAllViews()

        if (availableLabels.isEmpty()) {
            tvLabelsTitle.text = "选择标签（暂无标签，请先添加）"
            return
        }

        tvLabelsTitle.text = "选择标签"

        availableLabels.forEach { label ->
            val chip = Chip(this).apply {
                text = label.name
                isCheckable = true
                isClickable = true
                chipBackgroundColor = if (label.color != null) {
                    ColorStateList.valueOf(getColorFromString(label.color))
                } else {
                    getColorStateList(R.color.teal_200)
                }

                label.color?.let { color ->
                    setTextColor(getTextColorForBackground(getColorFromString(color)))
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedLabelIds.add(label.id)
                    } else {
                        selectedLabelIds.remove(label.id)
                    }
                }
            }
            chipGroupLabels.addView(chip)
        }
    }

    private fun addNewLabel() {
        val labelName = etNewLabel.text?.toString()?.trim()

        if (labelName.isNullOrEmpty()) {
            Snackbar.make(btnAddLabel, "请输入标签名称", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (labelName.length > 20) {
            Snackbar.make(btnAddLabel, "标签名称不能超过20个字符", Snackbar.LENGTH_SHORT).show()
            return
        }

        viewModel.createLabel(
            userId = RecordViewModel.DEFAULT_USER_ID,
            name = labelName,
            color = getRandomColor()
        )
    }

    private fun saveRecord() {
        val note = etNote.text?.toString()?.trim()?.ifEmpty { null }

        viewModel.createRecord(
            userId = RecordViewModel.DEFAULT_USER_ID,
            note = note,
            labelIds = selectedLabelIds.toList()
        )
    }

    private fun getColorFromString(colorString: String): Int {
        return try {
            android.graphics.Color.parseColor(colorString)
        } catch (e: Exception) {
            android.graphics.Color.GRAY
        }
    }

    private fun getTextColorForBackground(backgroundColor: Int): Int {
        val red = android.graphics.Color.red(backgroundColor)
        val green = android.graphics.Color.green(backgroundColor)
        val blue = android.graphics.Color.blue(backgroundColor)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000

        return if (brightness > 128) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }
    }

    private fun getRandomColor(): String {
        val colors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
            "#FFEB3B", "#FFC107", "#FF9800", "#FF5722"
        )
        return colors.random()
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

    override fun onDestroy() {
        super.onDestroy()
        updateTimeJob?.cancel()
    }
}
