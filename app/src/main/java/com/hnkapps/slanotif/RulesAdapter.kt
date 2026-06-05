package com.hnkapps.slanotif

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RulesAdapter(
    private var rules: MutableList<NotificationRule>,
    private val onDelete: (NotificationRule) -> Unit,
    private val onEdit: (NotificationRule) -> Unit
) : RecyclerView.Adapter<RulesAdapter.RuleViewHolder>() {

    class RuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvChannelName: TextView = view.findViewById(R.id.tv_channel_name)
        val tvSoundName: TextView = view.findViewById(R.id.tv_sound_name)
        val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.tvChannelName.text = rule.channelName
        holder.tvSoundName.text = "Sound: ${rule.soundName ?: "Default Tone"}"
        
        holder.btnDelete.setOnClickListener { onDelete(rule) }
        holder.itemView.setOnClickListener { onEdit(rule) }
    }

    override fun getItemCount() = rules.size

    fun updateRules(newRules: List<NotificationRule>) {
        rules = newRules.toMutableList()
        notifyDataSetChanged()
    }
}
