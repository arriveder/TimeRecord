package com.example.timerecord.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timerecord.R

data class DateStat(
    val date: String,
    val count: Int
)

class DateStatAdapter : RecyclerView.Adapter<DateStatAdapter.DateStatViewHolder>() {

    private var items = listOf<DateStat>()

    fun submitList(newItems: List<DateStat>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateStatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date_stat, parent, false)
        return DateStatViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateStatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class DateStatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        private val tvCount: TextView = itemView.findViewById(R.id.tv_count)

        fun bind(item: DateStat) {
            tvDate.text = item.date
            tvCount.text = "${item.count} 条"
        }
    }
}
