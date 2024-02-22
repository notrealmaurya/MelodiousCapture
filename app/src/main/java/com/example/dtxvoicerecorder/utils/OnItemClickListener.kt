package com.example.dtxvoicerecorder.utils


interface OnItemClickListener {
    fun onItemClickListener(position: Int)
    fun onItemLongClickListener(position: Int)

    fun onItemCheckedChange(position: Int, isChecked: Boolean)
}


