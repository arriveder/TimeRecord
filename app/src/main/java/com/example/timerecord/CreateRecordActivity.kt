package com.example.timerecord

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.timerecord.auth.AuthManager
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

class CreateRecordActivity : BaseActivity() {

    private val viewModel: RecordViewModel by viewModels()
    private val selectedLabelIds = mutableSetOf<String>()
    private val availableLabels = mutableListOf<Label>()
    private var updateTimeJob: Job? = null
    private lateinit var authManager: AuthManager

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

        authManager = AuthManager.getInstance(this)

        // 检查用户是否已登录
        if (!authManager.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

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
            val labelColor = if (label.color != null) {
                getColorFromString(label.color)
            } else {
                getColorFromString("#80CBC4")
            }

            val chip = Chip(this).apply {
                text = label.name
                isCheckable = true
                isClickable = true

                chipStrokeColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        Color.TRANSPARENT,
                        labelColor
                    )
                )

                chipBackgroundColor = ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf()
                    ),
                    intArrayOf(
                        labelColor,
                        Color.TRANSPARENT
                    )
                )

                setTextColor(labelColor)

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedLabelIds.add(label.id)
                        setTextColor(getTextColorForBackground(labelColor))
                    } else {
                        selectedLabelIds.remove(label.id)
                        setTextColor(labelColor)
                    }
                }
            }

            if (selectedLabelIds.contains(label.id)) {
                chip.setTextColor(getTextColorForBackground(labelColor))
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

        viewModel.createLabelWithSync(
            name = labelName,
            color = getRandomColor()
        )
    }

    private fun saveRecord() {
        val note = etNote.text?.toString()?.trim()?.ifEmpty { null }

        viewModel.createRecordWithSync(
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
            // 莫兰迪色系 - 低饱和度、柔和的颜色
            "#B39DDB", // 柔和紫
            "#90CAF9", // 柔和蓝
            "#80CBC4", // 柔和青
            "#A5D6A7", // 柔和绿
            "#FFF59D", // 柔和黄
            "#FFCC80", // 柔和橙
            "#F48FB1", // 柔和粉
            "#BCAAA4", // 柔和棕
            "#D7CCC8", // 柔和灰棕
            "#C5CAE9", // 柔和靛蓝
            "#CE93D8", // 柔和紫红
            "#81C784"  // 柔和草绿
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
