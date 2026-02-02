package com.example.tugasakhir.ui.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tugasakhir.adapter.HistoryAdapter
import com.example.tugasakhir.database.HistoryRepository
import com.example.tugasakhir.databinding.FragmentHistoryBinding
import com.example.tugasakhir.model.HistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryFragment : Fragment(),
    HistoryAdapter.OnDeleteClickListener {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    // Variable History Adapter
    private lateinit var historyAdapter: HistoryAdapter
    // Variable History Repository
    private lateinit var historyRepository: HistoryRepository
    // Data list Model
    private val historyList = mutableListOf<HistoryModel>()

    companion object {
        const val TAG = "HistoryFragment"
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

        // Setup history repository
        historyRepository = HistoryRepository(requireContext())

        setupRecyclerView()
        loadHistoryFromSQLite()
    }

    // Function for Recycler View Data
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(historyList)
        historyAdapter.setOnDeleteClickListener(this)

        // Integration Layout RecyclerView
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
            setHasFixedSize(true)
        }
    }

    // LOAD DATA HISTORY FROM SQLite
    @SuppressLint("NotifyDataSetChanged")
    private fun loadHistoryFromSQLite() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            //  Get all data history
            val history = historyRepository.getAllHistory()
            Log.d(TAG, "Total history: ${history.size}")

            withContext(Dispatchers.Main) {
                historyList.clear()
                historyList.addAll(history)
                historyAdapter.notifyDataSetChanged()
                showOrHideNoHistoryText()
            }
        }
    }

    // Function Show or hide text No History data
    private fun showOrHideNoHistoryText() {
        binding.tvHistoryNotFound.visibility =
            if (historyList.isEmpty()) View.VISIBLE else View.GONE

        binding.rvHistory.visibility =
            if (historyList.isEmpty()) View.GONE else View.VISIBLE
    }

    // Function for delete click
    override fun onDeleteClick(position: Int) {
        val history = historyList[position]

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            historyRepository.deleteHistoryById(history.id)

            withContext(Dispatchers.Main) {
                historyList.removeAt(position)
                historyAdapter.notifyItemRemoved(position)
                showOrHideNoHistoryText()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
