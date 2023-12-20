package com.example.dtxvoicerecorder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dtxvoicerecorder.databinding.RecordedItemBinding
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date

class RecorderFilesAdapter(
    private val context: Context,
    private var listener: OnItemClickListener,
    private val itemList: ArrayList<RecorderData>
) : RecyclerView.Adapter<RecorderFilesAdapter.RecorderFileHolder>() {

    private var editMode = false
    fun isEditMode(): Boolean {
        return editMode
    }

    fun setEditMode(mode: Boolean) {
        if (editMode != mode) {
            editMode = mode
            notifyDataSetChanged()
        }
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd/MM/yyyy")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecorderFileHolder {
        return RecorderFileHolder(
            RecordedItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecorderFileHolder, position: Int) {
        if (position != RecyclerView.NO_POSITION) {

            val record = itemList[position]
            val date = Date(record.timestamp)
            holder.AudioName.text = record.fileName
            holder.AudioDate.text = dateFormat.format(date)
            holder.AudioDuration.text = record.duration


            if (editMode) {
                holder.chekbox.isClickable=false
                holder.chekbox.visibility = View.VISIBLE
                holder.chekbox.isChecked = record.isChecked
            }else{
                holder.chekbox.visibility = View.GONE
                holder.chekbox.isChecked = false
            }


        }
    }


    fun updateData(newData: List<RecorderData>) {
        itemList.clear()
        itemList.addAll(newData)
    }


    override fun getItemCount(): Int {
        return itemList.size
    }


    inner class RecorderFileHolder(binding: RecordedItemBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {
        val AudioName = binding.AudioName
        val AudioDate = binding.AudioDate
        val AudioDuration = binding.AudioDuration
        val root = binding.root
        val chekbox = binding.checkboxMusicItem

        init {
            root.setOnClickListener(this)
            root.setOnLongClickListener(this)
        }

        override fun onClick(p0: View?) {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemClickListener(position)
            }
        }

        override fun onLongClick(p0: View?): Boolean {
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                listener.onItemLongClickListener(position)
            }
            return true
        }


    }
}