package com.example.timerecord.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timerecord.R

data class LabelStat(
    val labelName: String,
    val labelColor: String?,
    val count: Int
)

class LabelStatAdapter : RecyclerView.Adapter<LabelStatAdapter.LabelStatViewHolder>() {

    private var items = listOf<LabelStat>()

    fun submitList(newItems: List<LabelStat>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun getColorFromString(colorString: String): Int {
        return try {
            Color.parseColor(colorString)
        } catch (e: Exception) {
            Color.GRAY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_label_stat, parent, false)
        return LabelStatViewHolder(view)
    }

    override fun onBindViewHolder(holder: LabelStatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LabelStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewColor: View = itemView.findViewById(R.id.view_color)
        private val tvLabelName: TextView = itemView.findViewById(R.id.tv_label_name)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)

        fun bind(item: LabelStat) {
            tvLabelName.text = item.labelName
            tvCount.text = "${item.count} 次"

            val bgColor = if (item.labelColor != null) {
                getColorFromString(item.labelColor)
            } else {
                Color.parseColor("#03A9F4")
            }
            viewColor.setBackgroundColor(bgColor)
        }
    }
}
