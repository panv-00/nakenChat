package com.example.nakenchat

import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        val textTimestamp: TextView = itemView.findViewById(R.id.textTimestamp)
    }

    override fun getItemViewType(position: Int): Int {
        return when (messages[position].type) {
            MessageType.SENT, MessageType.RECEIVED, MessageType.PRIVATE_IN, MessageType.PRIVATE_OUT -> 1
            else -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes = if (viewType == 1) {
            R.layout.item_message_bubble
        } else {
            R.layout.item_message_plain
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return MessageViewHolder(view)
    }


    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context

        val content = buildSpannableForMessage(message, context)
        holder.textMessage.text = content

        val bubble = holder.itemView.findViewById<View>(R.id.messageBubble)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val timeText = timeFormat.format(Date(message.timestamp))

        val senderInfo = when (message.type) {
            MessageType.SENT -> "( ${message.fromId} ) - You"
            MessageType.RECEIVED -> "( ${message.fromId} ) - ${message.from}"
            MessageType.PRIVATE_IN -> "( ${message.fromId} ) - ${message.from} -> You"
            MessageType.PRIVATE_OUT -> "You -> ( ${message.toId} ) - ${message.to}"
            else -> null
        }

        holder.textTimestamp.text = if (senderInfo != null) {
            "$timeText • $senderInfo"
        } else {
            timeText
        }

        if (bubble != null) {
            // This is a bubble message (sent, received, private)
            val layoutParams = bubble.layoutParams as FrameLayout.LayoutParams
            when (message.type) {
                MessageType.SENT, MessageType.PRIVATE_OUT -> {
                    bubble.setBackgroundResource(R.drawable.message_bubble_sent)
                    layoutParams.gravity = android.view.Gravity.END
                    holder.textMessage.textSize = 16f
                    holder.textMessage.typeface = Typeface.DEFAULT
                    holder.textTimestamp.visibility = View.VISIBLE
                }
                MessageType.RECEIVED, MessageType.PRIVATE_IN -> {
                    bubble.setBackgroundResource(R.drawable.message_bubble_received)
                    layoutParams.gravity = android.view.Gravity.START
                    holder.textMessage.textSize = 16f
                    holder.textMessage.typeface = Typeface.DEFAULT
                    holder.textTimestamp.visibility = View.VISIBLE
                }
                else -> {
                    // Should not happen, but fallback
                    bubble.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    holder.textTimestamp.visibility = View.GONE
                }
            }
            bubble.layoutParams = layoutParams
            bubble.visibility = View.VISIBLE
        } else {
            // No bubble in this layout: system, none, emote messages
            holder.textMessage.textSize = 12f
            holder.textMessage.typeface = Typeface.MONOSPACE
            holder.textTimestamp.visibility = View.GONE
        }

        // Adjust bottom margin
        val itemLayoutParams = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        itemLayoutParams.bottomMargin = when (message.type) {
            MessageType.SYSTEM, MessageType.NONE, MessageType.EMOTE -> 0
            else -> 24
        }
        holder.itemView.layoutParams = itemLayoutParams
    }

    override fun getItemCount(): Int = messages.size

    private fun buildSpannableForMessage(message: Message, context: android.content.Context): SpannableString {
        val rawText = when (message.type) {
            MessageType.SYSTEM -> ">> ${message.text}"
            MessageType.SENT,
            MessageType.RECEIVED,
            MessageType.PRIVATE_IN,
            MessageType.NONE,
            MessageType.PRIVATE_OUT -> message.text
            MessageType.EMOTE -> "* ${message.from} ${message.text}"
        }

        val spannable = SpannableString(rawText)

        val color = when (message.type) {
            MessageType.SYSTEM -> ContextCompat.getColor(context, android.R.color.holo_red_light)
            MessageType.SENT -> ContextCompat.getColor(context, android.R.color.holo_blue_dark)
            MessageType.RECEIVED -> ContextCompat.getColor(context, android.R.color.black)
            MessageType.PRIVATE_IN -> ContextCompat.getColor(context, android.R.color.holo_purple)
            MessageType.PRIVATE_OUT -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            MessageType.EMOTE -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
            MessageType.NONE -> ContextCompat.getColor(context, android.R.color.holo_red_light)
        }

        // Apply text color span
        spannable.setSpan(
            ForegroundColorSpan(color),
            0,
            spannable.length,
            SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannable
    }
}