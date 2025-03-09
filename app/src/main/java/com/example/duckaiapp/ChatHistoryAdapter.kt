package com.example.duckaiapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatHistoryAdapter(
    private val chats: List<ChatHistoryDatabase.ChatEntry>,
    private val onItemClick: (ChatHistoryDatabase.ChatEntry) -> Unit
) : RecyclerView.Adapter<ChatHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timestampTextView: TextView = view.findViewById(R.id.timestampTextView)
        val previewTextView: TextView = view.findViewById(R.id.previewTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        holder.timestampTextView.text = chat.timestamp
        
        // Get a preview of the encrypted content
        val encryptedPreview = "Encrypted chat (tap to view)"
        holder.previewTextView.text = encryptedPreview
        
        holder.itemView.setOnClickListener {
            onItemClick(chat)
        }
    }

    override fun getItemCount() = chats.size
}
