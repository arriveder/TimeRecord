package com.example.timerecord

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timerecord.adapter.LabelAdapter
import com.example.timerecord.adapter.LabelWithUsage
import com.example.timerecord.databinding.ActivityLabelManagementBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LabelManagementActivity : AppCompatActivity() {

    private val viewModel: RecordViewModel by viewModels()
    private lateinit var binding: ActivityLabelManagementBinding
    private lateinit var labelAdapter: LabelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLabelManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadLabels()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "标签管理"
    }

    private fun setupRecyclerView() {
        labelAdapter = LabelAdapter()

        labelAdapter.onEditClickListener = { label ->
            showEditLabelDialog(label)
        }

        labelAdapter.onDeleteClickListener = { label ->
            showDeleteLabelDialog(label)
        }

        binding.recyclerViewLabels.apply {
            layoutManager = LinearLayoutManager(this@LabelManagementActivity)
            adapter = labelAdapter
        }
    }

    private fun loadLabels() {
        lifecycleScope.launch {
            try {
                val labels = viewModel.userLabels.value ?: emptyList()
                if (labels.isEmpty()) {
                    // Load from repository if LiveData is empty
                    viewModel.loadLabels()
                    viewModel.userLabels.observe(this@LabelManagementActivity) { loadedLabels ->
                        updateLabelList(loadedLabels)
                    }
                } else {
                    updateLabelList(labels)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LabelManagementActivity, "加载标签失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateLabelList(labels: List<com.example.timerecord.entity.Label>) {
        lifecycleScope.launch {
            val labelsWithUsage = labels.map { label ->
                val usageCount = viewModel.getLabelUsageCount(label.id)
                LabelWithUsage(label, usageCount)
            }

            if (labelsWithUsage.isEmpty()) {
                binding.recyclerViewLabels.visibility = android.view.View.GONE
                binding.tvEmpty.visibility = android.view.View.VISIBLE
            } else {
                binding.recyclerViewLabels.visibility = android.view.View.VISIBLE
                binding.tvEmpty.visibility = android.view.View.GONE
                labelAdapter.submitList(labelsWithUsage)
            }
        }
    }

    private fun showEditLabelDialog(label: com.example.timerecord.entity.Label) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_label, null)
        val etLabelName = dialogView.findViewById<TextInputEditText>(R.id.et_label_name)

        etLabelName.setText(label.name)

        builder.setView(dialogView)
            .setTitle("编辑标签")
            .setPositiveButton("保存") { _, _ ->
                val newName = etLabelName.text?.toString()?.trim()
                if (newName.isNullOrEmpty()) {
                    Toast.makeText(this, "标签名称不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newName.length > 20) {
                    Toast.makeText(this, "标签名称不能超过20个字符", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.updateLabel(label.id, newName)
            }
            .setNegativeButton("取消", null)
            .show()

        // Observe update result
        viewModel.createLabelResult.observe(this) { result ->
            result.onSuccess {
                Snackbar.make(binding.root, "标签更新成功", Snackbar.LENGTH_SHORT).show()
                loadLabels()
            }.onFailure { error ->
                val message = when (error.message) {
                    "Label name already exists" -> "标签名称已存在"
                    else -> "更新失败: ${error.message}"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteLabelDialog(label: com.example.timerecord.entity.Label) {
        lifecycleScope.launch {
            val usageCount = viewModel.getLabelUsageCount(label.id)
            val message = if (usageCount > 0) {
                "确定要删除标签 \"${label.name}\" 吗？\n该标签正在被 $usageCount 条记录使用，删除后这些记录将不再包含此标签。"
            } else {
                "确定要删除标签 \"${label.name}\" 吗？"
            }

            AlertDialog.Builder(this@LabelManagementActivity)
                .setTitle("删除确认")
                .setMessage(message)
                .setPositiveButton("删除") { _, _ ->
                    viewModel.deleteLabel(label.id)
                }
                .setNegativeButton("取消", null)
                .show()
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
}
