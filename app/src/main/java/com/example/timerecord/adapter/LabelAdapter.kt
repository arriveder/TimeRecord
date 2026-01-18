package com.example.timerecord.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.timerecord.R
import com.example.timerecord.entity.Label

data class LabelWithUsage(
    val label: Label,
    val usageCount: Int
)

class LabelAdapter : ListAdapter<LabelWithUsage, LabelAdapter.LabelViewHolder>(LabelDiffCallback()) {

    var onEditClickListener: ((Label) -> Unit)? = null
    var onDeleteClickListener: ((Label) -> Unit)? = null

    private fun getColorFromString(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_label, parent, false)
        return LabelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColorIndicator: View = itemView.findViewById(R.id.view_color_indicator)
        private val tvLabelName: TextView = itemView.findViewById(R.id.tv_label_name)
        private val tvUsageCount: TextView = itemView.findViewById(R.id.tv_usage_count)
        private val btnEdit: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btn_delete)

        fun bind(item: LabelWithUsage) {
            val label = item.label

            tvLabelName.text = label.name
            tvUsageCount.text = "使用 ${item.usageCount} 次"

            // Set color indicator
            val bgColor = if (label.color != null) {
                getColorFromString(label.color)
            } else {
                Color.parseColor("#03A9F4")
            }
            viewColorIndicator.setBackgroundColor(bgColor)

            // Edit button
            btnEdit.setOnClickListener {
                onEditClickListener?.invoke(label)
            }

            // Delete button
            btnDelete.setOnClickListener {
                onDeleteClickListener?.invoke(label)
            }
        }
    }

    class LabelDiffCallback : DiffUtil.ItemCallback<LabelWithUsage>() {
        override fun areItemsTheSame(oldItem: LabelWithUsage, newItem: LabelWithUsage): Boolean {
            return oldItem.label.id == newItem.label.id
        }

        override fun areContentsTheSame(oldItem: LabelWithUsage, newItem: LabelWithUsage): Boolean {
            return oldItem == newItem
        }
    }
}
