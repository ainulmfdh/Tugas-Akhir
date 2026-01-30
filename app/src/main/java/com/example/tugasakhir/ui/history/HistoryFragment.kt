package com.example.tugasakhir.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tugasakhir.adapter.HistoryAdapter
import com.example.tugasakhir.database.History
import com.example.tugasakhir.database.HistoryDatabase
import com.example.tugasakhir.databinding.FragmentHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment(),
    HistoryAdapter.OnDeleteClickListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyAdapter: HistoryAdapter
    private val historyList: MutableList<History> = mutableListOf()

    companion object {
        const val TAG = "historydata"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadHistoryFromDatabase()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList)
        historyAdapter.setOnDeleteClickListener(this)

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadHistoryFromDatabase() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val history = HistoryDatabase
                .getDatabase(requireContext())
                .historyDao()
                .getAllHistory()

            Log.d(TAG, "Number of predictions: ${history.size}")

            withContext(Dispatchers.Main) {
                historyList.clear()
                historyList.addAll(history)
                historyAdapter.notifyDataSetChanged()
                showOrHideNoHistoryText()
            }
        }
    }

    private fun showOrHideNoHistoryText() {
        if (historyList.isEmpty()) {
            binding.tvHistoryNotFound.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.tvHistoryNotFound.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
        }
    }

    override fun onDeleteClick(position: Int) {
        val history = historyList[position]

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            HistoryDatabase
                .getDatabase(requireContext())
                .historyDao()
                .deleteHistory(history)

            withContext(Dispatchers.Main) {
                historyList.removeAt(position)
                historyAdapter.notifyDataSetChanged()
                showOrHideNoHistoryText()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

