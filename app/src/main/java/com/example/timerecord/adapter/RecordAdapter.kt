package com.example.timerecord.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timerecord.R
import com.example.timerecord.RecordWithLabels
import com.google.android.material.chip.Chip

class RecordAdapter : ListAdapter<RecordWithLabels, RecordAdapter.RecordViewHolder>(RecordDiffCallback()) {

    private var selectionMode: Boolean = false
    private val selectedIds = mutableSetOf<String>()
    var onSelectionChanged: ((Int) -> Unit)? = null
    var onItemClickListener: ((String) -> Unit)? = null

    private fun getColorFromString(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    private fun getTextColorForBackground(backgroundColor: Int): Int {
        val red = Color.red(backgroundColor)
        val green = Color.green(backgroundColor)
        val blue = Color.blue(backgroundColor)
        val brightness = (red * 299 + green * 587 + blue * 114) / 1000

        return if (brightness > 128) {
            Color.BLACK
        } else {
            Color.WHITE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectionMode, selectedIds.contains(item.record.id))
    }

    fun setSelectionMode(mode: Boolean) {
        if (selectionMode != mode) {
            selectionMode = mode
            if (!mode) {
                selectedIds.clear()
            }
            onSelectionChanged?.invoke(selectedIds.size)
            notifyDataSetChanged()
        }
    }

    fun toggleSelection(recordId: String) {
        if (selectedIds.contains(recordId)) {
            selectedIds.remove(recordId)
        } else {
            selectedIds.add(recordId)
        }
        onSelectionChanged?.invoke(selectedIds.size)
        notifyDataSetChanged()
    }

    fun selectAll() {
        currentList.forEach { item ->
            selectedIds.add(item.record.id)
        }
        onSelectionChanged?.invoke(selectedIds.size)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedIds.clear()
        onSelectionChanged?.invoke(0)
        notifyDataSetChanged()
    }

    fun getSelectedIds(): Set<String> = selectedIds.toSet()

    fun getSelectedCount(): Int = selectedIds.size

    inner class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvNote: TextView = itemView.findViewById(R.id.tv_note)
        private val chipGroupLabels: com.google.android.material.chip.ChipGroup = itemView.findViewById(R.id.chip_group_labels)
        private val tvEmptyLabels: TextView = itemView.findViewById(R.id.tv_empty_labels)
        private val ivSelectionDot: ImageView = itemView.findViewById(R.id.iv_selection_dot)

        fun bind(item: RecordWithLabels, selectionMode: Boolean, isSelected: Boolean) {
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

                        val bgColor = if (label.color != null) {
                            this@RecordAdapter.getColorFromString(label.color)
                        } else {
                            Color.parseColor("#03A9F4")
                        }
                        chipBackgroundColor = ColorStateList.valueOf(bgColor)
                        setTextColor(this@RecordAdapter.getTextColorForBackground(bgColor))
                    }
                    chipGroupLabels.addView(chip)
                }
            }

            // Handle selection mode
            if (selectionMode) {
                ivSelectionDot.visibility = View.VISIBLE
                ivSelectionDot.setImageResource(if (isSelected) R.drawable.ic_selected else R.drawable.ic_unselected)
            } else {
                ivSelectionDot.visibility = View.GONE
            }

            // Handle click
            itemView.setOnClickListener {
                if (selectionMode) {
                    toggleSelection(record.id)
                } else {
                    onItemClickListener?.invoke(record.id)
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
