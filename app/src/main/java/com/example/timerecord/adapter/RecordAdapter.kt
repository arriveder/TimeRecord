package com.example.timerecord.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timerecord.R
import com.example.timerecord.RecordWithLabels
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

class RecordAdapter : ListAdapter<RecordWithLabels, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvNote: TextView = itemView.findViewById(R.id.tv_note)
        private val chipGroupLabels: com.google.android.material.chip.ChipGroup = itemView.findViewById(R.id.chip_group_labels)
        private val tvEmptyLabels: TextView = itemView.findViewById(R.id.tv_empty_labels)

        fun bind(item: RecordWithLabels) {
            val record = item.record
            val labels = item.labels

            tvTime.text = record.time24
            tvDate.text = record.date

            if (record.note.isNullOrBlank()) {
                tvNote.visibility = View.GONE
            } else {
                tvNote.visibility = View.VISIBLE
                tvNote.text = record.note
            }

            chipGroupLabels.removeAllViews()

            if (labels.isEmpty()) {
                tvEmptyLabels.visibility = View.VISIBLE
            } else {
                tvEmptyLabels.visibility = View.GONE
                labels.forEach { label ->
                    val chip = Chip(itemView.context).apply {
                        text = label.name
                        isClickable = false
                        setChipBackgroundColorResource(android.R.color.holo_blue_light)
                        setTextColor(android.R.color.white)
                    }
                    chipGroupLabels.addView(chip)
                }
            }
        }
    }

    class RecordDiffCallback : DiffUtil.ItemCallback<RecordWithLabels>() {
        override fun areItemsTheSame(oldItem: RecordWithLabels, newItem: RecordWithLabels): Boolean {
            return oldItem.record.id == newItem.record.id
        }

        override fun areContentsTheSame(oldItem: RecordWithLabels, newItem: RecordWithLabels): Boolean {
            return oldItem == newItem
        }
    }
}
