package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.timerecord.adapter.RecordAdapter
import com.example.timerecord.databinding.ActivitySearchResultBinding
import com.example.timerecord.util.SearchHistoryManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchResultActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchResultBinding
    private lateinit var recordAdapter: RecordAdapter
    private lateinit var searchHistoryManager: SearchHistoryManager
    private var currentQuery: String = ""
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchHistoryManager = SearchHistoryManager(this)
        currentQuery = intent.getStringExtra(SearchActivity.EXTRA_SEARCH_QUERY) ?: ""

        setupBackButton()
        setupRecyclerView()
        setupSearchBox()
        loadSearchResults(currentQuery)
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recordAdapter = RecordAdapter()
        recordAdapter.onItemClickListener = { recordId ->
            val intent = Intent(this, RecordDetailActivity::class.java).apply {
                putExtra(RecordDetailActivity.EXTRA_RECORD_ID, recordId)
            }
            startActivity(intent)
        }

        binding.recyclerViewResults.apply {
            layoutManager = LinearLayoutManager(this@SearchResultActivity)
            adapter = recordAdapter
        }
    }

    private fun setupSearchBox() {
        // 显示当前搜索词
        binding.etSearch.setText(currentQuery)

        // 监听键盘搜索动作
        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotBlank()) {
                    searchHistoryManager.addHistory(query)
                    currentQuery = query
                    loadSearchResults(query)
                }
                true
            } else {
                false
            }
        }

        // 监听文本变化，实现实时搜索（带防抖）
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 取消之前的搜索任务
                searchJob?.cancel()

                // 防抖 500ms
                searchJob = lifecycleScope.launch {
                    delay(500)
                    val query = s?.toString()?.trim() ?: ""
                    if (query.isNotBlank() && query != currentQuery) {
                        searchHistoryManager.addHistory(query)
                        currentQuery = query
                        loadSearchResults(query)
                    }
                }
            }
        })

        // 清除按钮
        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }

        // 初始化清除按钮状态
        binding.btnClear.visibility = if (currentQuery.isBlank()) View.GONE else View.VISIBLE
    }

    private fun loadSearchResults(query: String) {
        lifecycleScope.launch {
            try {
                val viewModel = RecordViewModel(application)
                val recordsWithLabels = viewModel.searchRecords(query, RecordViewModel.DEFAULT_USER_ID)

                binding.tvResultCount.text = "找到 ${recordsWithLabels.size} 条记录"

                if (recordsWithLabels.isEmpty()) {
                    binding.recyclerViewResults.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewResults.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                    recordAdapter.submitList(recordsWithLabels)
                }
            } catch (e: Exception) {
                binding.recyclerViewResults.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    companion object {
    }
}
