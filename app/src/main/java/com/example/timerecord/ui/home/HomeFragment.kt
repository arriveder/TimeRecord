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
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordViewModel: RecordViewModel
    private lateinit var recordAdapter: RecordAdapter

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
        loadRecords()
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter()

        binding.recyclerViewRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recordAdapter
        }
    }

    private fun loadRecords() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recordsWithLabels = recordViewModel.getRecordsWithLabels(
                    RecordViewModel.DEFAULT_USER_ID
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
        loadRecords()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
