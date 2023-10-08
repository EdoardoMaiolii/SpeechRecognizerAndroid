package com.edoardo.speechrecognizer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.edoardo.speechrecognizer.model.Output

class OutputAdapter  :
    RecyclerView.Adapter<OutputAdapter.OutputHolder>() {

    private var outputs: ArrayList<Output> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutputHolder {
        val view: View =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.output_card, parent, false)

        return OutputHolder(view)
    }

    override fun onBindViewHolder(holder: OutputHolder, position: Int) {
        val current: Output = outputs[position]

        holder.textCommand.text = current.command
        holder.textValue.text = current.value
    }

    inner class OutputHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCommand: TextView = itemView.findViewById(R.id.command_text)
        val textValue: TextView = itemView.findViewById(R.id.value_text)
    }

    override fun getItemCount(): Int {
        return outputs.size as Int
    }

    fun addOutputs(outputs: ArrayList<Output>) {
        val prevOutputsCount = this.outputs.size
        this.outputs.addAll(outputs)
        notifyItemRangeInserted(prevOutputsCount + 1, outputs.size)
    }

}