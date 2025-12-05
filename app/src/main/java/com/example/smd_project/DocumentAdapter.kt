package com.example.smd_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DocumentAdapter(
    private var items: List<DocumentEntity> = emptyList(),
    private val onClick: (DocumentEntity) -> Unit
) : RecyclerView.Adapter<DocumentAdapter.VH>() {

    fun submitList(newList: List<DocumentEntity>) {
        items = newList
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvSub: TextView = itemView.findViewById(R.id.tvSub)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvAction: TextView = itemView.findViewById(R.id.tvAction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.tvName.text = d.originalName
        holder.tvSub.text = d.extension?.let { it.uppercase() } ?: "File"
        holder.tvDate.text = d.createdAt ?: ""
        // set icon depending on extension (pdf / image / docx)
        val ext = d.extension?.lowercase() ?: ""
        val iconRes = when {
            ext.contains("pdf") -> R.drawable.ic_file_pdf
            ext.startsWith("jpg") || ext.startsWith("png") || ext.startsWith("jpeg") -> R.drawable.ic_file_image
            ext.contains("doc") -> R.drawable.ic_file_docx
            else -> R.drawable.ic_file_docx
        }
        holder.ivIcon.setImageResource(iconRes)

        holder.itemView.setOnClickListener { onClick(d) }
        holder.tvAction.setOnClickListener { onClick(d) }
    }

    override fun getItemCount(): Int = items.size
}
