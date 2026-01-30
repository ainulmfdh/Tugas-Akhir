package com.example.tugasakhir.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tugasakhir.R
import com.example.tugasakhir.database.History
import java.io.File

class HistoryAdapter(
    private val historyList: List<History>
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var onDeleteClickListener: OnDeleteClickListener? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView =
            itemView.findViewById(R.id.image_history)

        private val labelTextView: TextView =
            itemView.findViewById(R.id.labelHistory)

        private val descriptionTextView: TextView =
            itemView.findViewById(R.id.descriptionHistory)

        private val dateTextView: TextView =
            itemView.findViewById(R.id.textDate)

        private val deleteButton: ImageButton =
            itemView.findViewById(R.id.btnDelete)

        fun bind(history: History) {

            // Load image
            Glide.with(itemView.context)
                .load(File(history.imagePath))
                .placeholder(R.drawable.illustration_upload)
                .error(R.drawable.ic_launcher_background)
                .into(imageView)

            // Set data
            labelTextView.text = history.label
            descriptionTextView.text = history.description
            dateTextView.text = history.createdAt

            deleteButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClickListener?.onDeleteClick(position)
                }
            }
        }
    }

    fun setOnDeleteClickListener(listener: OnDeleteClickListener) {
        onDeleteClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(historyList[position])
    }

    override fun getItemCount(): Int = historyList.size

    interface OnDeleteClickListener {
        fun onDeleteClick(position: Int)
    }
}
