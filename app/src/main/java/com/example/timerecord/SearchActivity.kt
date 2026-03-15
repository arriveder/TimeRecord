package com.example.timerecord

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.example.timerecord.databinding.ActivitySearchBinding
import com.example.timerecord.util.SearchHistoryManager
import com.google.android.material.chip.Chip

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchHistoryManager: SearchHistoryManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchHistoryManager = SearchHistoryManager(this)

        setupBackButton()
        setupSearchBox()
        setupClearButton()
        loadSearchHistory()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupSearchBox() {
        // 自动聚焦搜索框并显示键盘
        binding.etSearch.postDelayed({
            binding.etSearch.isFocusableInTouchMode = true
            binding.etSearch.requestFocus()
            showKeyboard()
        }, 300)

        // 监听键盘搜索动作
        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotBlank()) {
                    searchHistoryManager.addHistory(query)
                    navigateToSearchResult(query)
                }
                true
            } else {
                false
            }
        }
    }

    private fun setupClearButton() {
        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
        }

        // 监听文本变化，控制清除按钮显示/隐藏
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.btnClear.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
            }
        })
    }

    private fun loadSearchHistory() {
        val history = searchHistoryManager.getHistory()

        // 清空历史点击事件
        binding.tvClearHistory.setOnClickListener {
            clearSearchHistory()
        }

        if (history.isEmpty()) {
            binding.tvEmptyHistory.visibility = View.VISIBLE
            binding.chipGroupHistory.visibility = View.GONE
        } else {
            binding.tvEmptyHistory.visibility = View.GONE
            binding.chipGroupHistory.visibility = View.VISIBLE

            binding.chipGroupHistory.removeAllViews()
            history.forEach { query ->
                val chip = Chip(this).apply {
                    text = query
                    isClickable = true
                    isCheckable = false
                    setOnClickListener {
                        // 点击历史标签，执行搜索
                        searchHistoryManager.addHistory(query)
                        navigateToSearchResult(query)
                    }
                }
                binding.chipGroupHistory.addView(chip)
            }
        }
    }

    private fun navigateToSearchResult(query: String) {
        // 隐藏键盘
        hideKeyboard()

        val intent = Intent(this, SearchResultActivity::class.java).apply {
            putExtra(EXTRA_SEARCH_QUERY, query)
        }
        startActivity(intent)
    }

    private fun clearSearchHistory() {
        searchHistoryManager.clearHistory()
        loadSearchHistory()
    }

    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    companion object {
        const val EXTRA_SEARCH_QUERY = "search_query"
    }
}
