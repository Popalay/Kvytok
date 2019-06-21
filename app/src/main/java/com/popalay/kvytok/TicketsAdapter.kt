package com.popalay.kvytok

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class TicketsAdapter : ListAdapter<Ticket, TicketsAdapter.ViewHolder>(TicketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = TicketLayout(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class ViewHolder(private val ticketLayout: TicketLayout) :
        RecyclerView.ViewHolder(ticketLayout) {


        fun bind(ticket: Ticket) {
            ticketLayout.ticket = ticket
        }
    }
}

class TicketDiffCallback : DiffUtil.ItemCallback<Ticket>() {

    override fun areItemsTheSame(oldItem: Ticket, newItem: Ticket): Boolean =
        oldItem.ticketNumber == newItem.ticketNumber

    override fun areContentsTheSame(oldItem: Ticket, newItem: Ticket): Boolean = oldItem == newItem
}