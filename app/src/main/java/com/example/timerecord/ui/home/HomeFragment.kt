package com.example.timerecord.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter()

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

    override fun onResume() {
        super.onResume()
        setupDateFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
