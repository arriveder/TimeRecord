package com.example.timerecord.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timerecord.R
import com.example.timerecord.RecordViewModel
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
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter()

        recordAdapter.onSelectionChanged = { count ->
            updateSelectAllIcon(count)
        }

        binding.recyclerViewRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }
    }

    private fun setupDateFilter() {
        binding.tvDateFilter.apply {
            text = "全部日期"

            setOnClickListener {
                showDatePicker()
            }
        }

        loadRecords(null)
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
                val recordsWithLabels = recordViewModel.getRecordsWithLabelsByDate(
                    RecordViewModel.DEFAULT_USER_ID,
                    selectedDate
                )

                if (recordsWithLabels.isEmpty()) {
                    binding.recyclerViewRecords.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewRecords.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    recordAdapter.submitList(recordsWithLabels)
                }
            } catch (e: Exception) {
                binding.recyclerViewRecords.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
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
                    recordViewModel.deleteRecordById(recordId)
                }

                // Exit selection mode
                selectionMode = false
                recordAdapter.setSelectionMode(false)
                binding.layoutBottomActions.visibility = View.GONE
                binding.btnSelectMode.setImageResource(R.drawable.ic_more)

                // Reload records
                loadRecords(selectedDate)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupDateFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
