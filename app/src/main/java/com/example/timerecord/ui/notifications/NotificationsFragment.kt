package com.example.timerecord.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timerecord.RecordViewModel
import com.example.timerecord.adapter.DateStat
import com.example.timerecord.adapter.DateStatAdapter
import com.example.timerecord.adapter.LabelStat
import com.example.timerecord.adapter.LabelStatAdapter
import com.example.timerecord.databinding.FragmentNotificationsBinding
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: RecordViewModel
    private lateinit var labelStatAdapter: LabelStatAdapter
    private lateinit var dateStatAdapter: DateStatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[RecordViewModel::class.java]

        setupRecyclerViews()
        loadStatistics()
    }

    private fun setupRecyclerViews() {
        labelStatAdapter = LabelStatAdapter()
        binding.recyclerViewLabelStats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = labelStatAdapter
        }

        dateStatAdapter = DateStatAdapter()
        binding.recyclerViewDateStats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dateStatAdapter
        }
    }

    private fun loadStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recordsWithLabels = viewModel.getRecordsWithLabels()

                // Calculate statistics
                val totalRecords = recordsWithLabels.size
                val todayDate = LocalDate.now().toString()
                val todayRecords = recordsWithLabels.count { it.record.date == todayDate }

                // Week records (last 7 days)
                val weekAgo = LocalDate.now().minusWeeks(1)
                val weekRecords = recordsWithLabels.count { recordWithLabels ->
                    try {
                        val recordDate = LocalDate.parse(recordWithLabels.record.date)
                        !recordDate.isBefore(weekAgo)
                    } catch (e: Exception) {
                        false
                    }
                }

                // Month records (current month)
                val now = LocalDate.now()
                val monthRecords = recordsWithLabels.count { recordWithLabels ->
                    try {
                        val recordDate = LocalDate.parse(recordWithLabels.record.date)
                        recordDate.month == now.month && recordDate.year == now.year
                    } catch (e: Exception) {
                        false
                    }
                }

                // Update summary cards
                binding.tvTotalRecords.text = totalRecords.toString()
                binding.tvTodayRecords.text = todayRecords.toString()
                binding.tvWeekRecords.text = weekRecords.toString()
                binding.tvMonthRecords.text = monthRecords.toString()

                // Label statistics
                val labelStats = recordsWithLabels
                    .flatMap { it.labels }
                    .groupBy { it.id }
                    .map { (labelId, labels) ->
                        val label = labels.first()
                        LabelStat(
                            labelName = label.name,
                            labelColor = label.color,
                            count = labels.size
                        )
                    }
                    .sortedByDescending { it.count }

                if (labelStats.isEmpty()) {
                    binding.recyclerViewLabelStats.visibility = View.GONE
                    binding.tvNoLabels.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewLabelStats.visibility = View.VISIBLE
                    binding.tvNoLabels.visibility = View.GONE
                    labelStatAdapter.submitList(labelStats)
                }

                // Date statistics
                val dateStats = recordsWithLabels
                    .map { it.record.date }
                    .groupingBy { it }
                    .eachCount()
                    .map { (date, count) -> DateStat(date, count) }
                    .sortedByDescending { it.date }

                if (dateStats.isEmpty()) {
                    binding.recyclerViewDateStats.visibility = View.GONE
                    binding.tvNoDates.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewDateStats.visibility = View.VISIBLE
                    binding.tvNoDates.visibility = View.GONE
                    dateStatAdapter.submitList(dateStats)
                }

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
