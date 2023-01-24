package com.aiwe.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiwe.app.databinding.ItemRvBinding

class Adapter(private val itemCallback: (Model?, View) -> Unit) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    var items: List<Model> = emptyList()
    set(value) {
        field = value
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val binding: ItemRvBinding) : RecyclerView.ViewHolder(binding.root) {

        private var item: Model? = null

        init {
            binding.itemMore.setOnClickListener { itemCallback(item, itemView) }
        }

        fun bind(item: Model) {
            this.item = item
            binding.itemText.text = item.string
        }
    }

    override fun getItemCount(): Int = items.size
}