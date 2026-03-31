package com.example.timerecord.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timerecord.LabelManagementActivity
import com.example.timerecord.RecordDetailActivity
import com.example.timerecord.RecordSortOption
import com.example.timerecord.RecordViewModel
import com.example.timerecord.R
import com.example.timerecord.SearchActivity
import com.example.timerecord.adapter.RecordAdapter
import com.example.timerecord.databinding.FragmentHomeBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordViewModel: RecordViewModel
    private lateinit var recordAdapter: RecordAdapter

    private var selectedDate: String? = null
    private var selectionMode: Boolean = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val sortOptions = RecordSortOption.entries.toTypedArray()
    private var currentSortIndex = 0

    companion object {
        private const val PREFS_NAME = "home_prefs"
        private const val KEY_SORT_OPTION = "sort_option"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recordViewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        setupRecyclerView()
        setupDateFilter()
        setupSelectionMode()
        setupBottomActions()

        // Observe sync state
        recordViewModel.isSyncing.observe(viewLifecycleOwner) { isSyncing ->
            if (_binding != null) {
                binding.swipeRefresh.isRefreshing = isSyncing
            }
        }

        recordViewModel.syncResult.observe(viewLifecycleOwner) { result ->
            if (_binding == null) return@observe

            if (result.isFailure) {
                // Show error message
                android.widget.Toast.makeText(
                    requireContext(),
                    "同步失败：${result.exceptionOrNull()?.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    requireContext(),
                    "同步完成",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            binding.swipeRefresh.isRefreshing = false

            // 同步完成后重新加载数据
            loadRecords(selectedDate)
        }

        // Load labels
        recordViewModel.loadLabels()
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter()

        recordAdapter.onSelectionChanged = { count ->
            updateSelectAllIcon(count)
        }

        recordAdapter.onItemClickListener = { recordId ->
            // Open detail activity
            val intent = Intent(requireContext(), RecordDetailActivity::class.java).apply {
                putExtra(RecordDetailActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }

        binding.recyclerViewRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }

        // Setup swipe refresh
        binding.swipeRefresh.setOnRefreshListener {
            recordViewModel.manualSync()
        }
        binding.swipeRefresh.setColorSchemeResources(
            R.color.primary,
            R.color.primary_variant,
            R.color.secondary
        )
    }

    private fun setupDateFilter() {
        binding.tvDateFilter.apply {
            text = "全部日期"

            setOnClickListener {
                showDatePicker()
            }
        }

        binding.btnSort.setOnClickListener {
            showSortOptionDialog()
        }

        binding.btnLabelManagement.setOnClickListener {
            val intent = Intent(requireContext(), LabelManagementActivity::class.java)
            startActivity(intent)
        }

        binding.btnSearch.setOnClickListener {
            val intent = Intent(requireContext(), SearchActivity::class.java)
            startActivity(intent)
        }

        // 加载保存的排序选项
        loadSavedSortOption()
        loadRecords(null)
    }

    private fun loadSavedSortOption() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentSortIndex = prefs.getInt(KEY_SORT_OPTION, 0)
        recordViewModel.setSortOption(sortOptions[currentSortIndex])
    }

    private fun saveSortOption(index: Int) {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SORT_OPTION, index).apply()
    }

    private fun showSortOptionDialog() {
        val sortTitles = sortOptions.map { it.title }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("选择排序方式")
            .setSingleChoiceItems(sortTitles, currentSortIndex) { dialog, which ->
                if (which != currentSortIndex) {
                    currentSortIndex = which
                    val sortOption = sortOptions[which]
                    recordViewModel.setSortOption(sortOption)
                    saveSortOption(which)
                    loadRecords(selectedDate)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(
                selectedDate?.let {
                    dateFormat.parse(it)?.time
                } ?: MaterialDatePicker.todayInUtcMilliseconds()
            )
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate = dateFormat.format(Date(selection))
            binding.tvDateFilter.text = selectedDate
            loadRecords(selectedDate)
        }

        datePicker.addOnNegativeButtonClickListener {
            // 用户点击取消，不执行任何操作
        }

        datePicker.addOnCancelListener {
            // 用户取消，不执行任何操作
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun loadRecords(selectedDate: String?) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 检查用户是否已登录
                if (!recordViewModel.isLoggedIn()) {
                    binding.recyclerViewRecords.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "请先登录\n点击下方 + 按钮去登录"
                    return@launch
                }

                val recordsWithLabels = recordViewModel.getRecordsWithLabelsByDate(
                    selectedDate,
                    sortOptions[currentSortIndex]
                )

                if (recordsWithLabels.isEmpty()) {
                    binding.recyclerViewRecords.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "暂无记录\n点击下方 + 按钮创建记录"
                } else {
                    binding.recyclerViewRecords.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    recordAdapter.submitList(recordsWithLabels)
                }
            } catch (e: Exception) {
                binding.recyclerViewRecords.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "加载失败：${e.message}"
            }
        }
    }

    private fun setupSelectionMode() {
        binding.btnSelectMode.setOnClickListener {
            selectionMode = !selectionMode
            recordAdapter.setSelectionMode(selectionMode)

            if (selectionMode) {
                binding.layoutBottomActions.visibility = View.VISIBLE
                binding.btnSelectMode.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            } else {
                binding.layoutBottomActions.visibility = View.GONE
                binding.btnSelectMode.setImageResource(R.drawable.ic_more)
            }
        }
    }

    private fun setupBottomActions() {
        binding.btnSelectAll.setOnClickListener {
            val currentCount = recordAdapter.getSelectedCount()
            val totalCount = recordAdapter.itemCount

            if (currentCount == totalCount) {
                // All selected, deselect all
                recordAdapter.clearSelection()
            } else {
                // Select all
                recordAdapter.selectAll()
            }
        }
        updateSelectAllIcon(recordAdapter.getSelectedCount())

        binding.btnDeleteSelected.setOnClickListener {
            val selectedIds = recordAdapter.getSelectedIds()
            if (selectedIds.isEmpty()) {
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("删除确认")
                .setMessage("确定要删除选中的 ${selectedIds.size} 条记录吗？")
                .setPositiveButton("删除") { _, _ ->
                    deleteSelectedRecords(selectedIds.toList())
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun updateSelectAllIcon(selectedCount: Int) {
        val totalCount = recordAdapter.itemCount
        if (selectedCount == totalCount && totalCount > 0) {
            binding.btnSelectAll.setIconResource(R.drawable.ic_selected)
            binding.btnSelectAll.text = "取消全选"
        } else {
            binding.btnSelectAll.setIconResource(R.drawable.ic_unselected)
            binding.btnSelectAll.text = "全选"
        }
    }

    private fun deleteSelectedRecords(recordIds: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                recordIds.forEach { recordId ->
                    recordViewModel.deleteRecordWithSync(recordId)
                }

                // 等待同步完成后退出选择模式
                selectionMode = false
                recordAdapter.setSelectionMode(false)
                binding.layoutBottomActions.visibility = View.GONE
                binding.btnSelectMode.setImageResource(R.drawable.ic_more)

                // 重新加载记录
                loadRecords(selectedDate)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadSavedSortOption()
        loadRecords(selectedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
