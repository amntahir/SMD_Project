package com.example.smd_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VerificationAdapter(
    private val items: MutableList<VerificationItem>,
    private val onApproveClicked: (VerificationItem) -> Unit,
    private val onRejectClicked: (VerificationItem) -> Unit
) : RecyclerView.Adapter<VerificationAdapter.Vh>() {

    inner class Vh(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_verification, parent, false)
        return Vh(view)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        val item = items[position]
        holder.tvName.text = item.name ?: "Unknown"
        holder.tvEmail.text = item.email ?: ""
        holder.tvStatus.text = "Status: ${item.status ?: "pending"}"

        holder.btnApprove.setOnClickListener { onApproveClicked(item) }
        holder.btnReject.setOnClickListener { onRejectClicked(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<VerificationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
