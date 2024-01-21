package com.example.dtxvoicerecorder

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.dtxvoicerecorder.database.AppDatabase
import com.example.dtxvoicerecorder.database.RecorderData
import com.example.dtxvoicerecorder.database.RecorderFilesAdapter
import com.example.dtxvoicerecorder.databinding.FragmentHomeBinding
import com.example.dtxvoicerecorder.utils.OnItemClickListener
import com.example.dtxvoicerecorder.utils.getFileExtension
import com.example.dtxvoicerecorder.utils.getFormattedDate
import com.example.dtxvoicerecorder.utils.getFormattedFileSize
import com.example.dtxvoicerecorder.utils.isExternalStorageWritable
import com.example.dtxvoicerecorder.utils.isFilePresent
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HomeFragment : Fragment(), OnItemClickListener {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private lateinit var records: ArrayList<RecorderData>
    private lateinit var db: AppDatabase
    private var isAllChecked = false
    private var isLongClickListenerInitialized = false
    private var filePath: String = ""

    companion object {
        lateinit var homeAdapter: RecorderFilesAdapter
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = fragmentHomeBinding.root

        records = ArrayList()
        db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "audioRecords").build()

        fragmentHomeBinding.recyclerViewHomeFragment.setHasFixedSize(true)
        fragmentHomeBinding.recyclerViewHomeFragment.setItemViewCacheSize(13)
        fragmentHomeBinding.recyclerViewHomeFragment.layoutManager =
            LinearLayoutManager(context)
        homeAdapter = RecorderFilesAdapter(requireContext(), this, records)
        fragmentHomeBinding.recyclerViewHomeFragment.adapter = homeAdapter


        // Assuming 'view' is the root view of your fragment
        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                if (fragmentHomeBinding.visibilityBottomToolbar.visibility == View.VISIBLE || fragmentHomeBinding.visibilityTopToolbar.visibility == View.VISIBLE) {
                    topToolbarCloseOptionFunction()
                    return@setOnKeyListener true // Consume the event
                }
            }
            false // Let the system handle the event
        }

        if (isExternalStorageWritable()) {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            filePath = File(downloadsFolder, "dtxVoicerecorder").absolutePath + "/"
        } else {
            filePath = "${requireActivity().filesDir.absolutePath}/"
        }
        isFilePresent(filePath)

        listeners()


        return view
    }

    private fun listeners() {

        //SearchView
        fragmentHomeBinding.searchViewHomeFragment.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchDatabase(newText.orEmpty())
                return true
            }
        })

        //RecordingButton
        fragmentHomeBinding.fragmentRecorderRecordingButton.setOnClickListener {
            val intent = Intent(activity, RecorderActivity::class.java)
            startActivity(intent)
        }

        //Toolbar CLose
        fragmentHomeBinding.cabTopToolbarInternal.topToolbarClose.setOnClickListener {
            topToolbarCloseOptionFunction()
        }

        //Toolbar SelectAll
        fragmentHomeBinding.cabTopToolbarInternal.topToolbarSelectedall.setOnClickListener {
            isAllChecked = !isAllChecked
            records.map { it.isChecked = isAllChecked }
            homeAdapter.notifyDataSetChanged()

            if (isAllChecked) {
                enableDeleteAndSend()
            } else {
                disableDeleteAndSend()
            }
            disableRenameAndDetails()
            fragmentHomeBinding.cabTopToolbarInternal.topToolbarSelectedall.setImageResource(
                if (isAllChecked) R.drawable.icon_selectall else R.drawable.icon_select_none
            )

        }


        //ToolbarShare
        fragmentHomeBinding.cabBottomToolbarHomeFragment.layoutBottomToolbarShare.setOnClickListener {
            val selectedFiles = records.filter { it.isChecked }

            if (selectedFiles.isNotEmpty()) {
                val fileUris = ArrayList<Uri>()
                val fileNames = ArrayList<String>()

                for (selectedFile in selectedFiles) {
                    val file = File(selectedFile.filePath)
                    val fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.provider",
                        file
                    )
                    fileUris.add(fileUri)
                    fileNames.add(file.name)
                }

                if (fileUris.isNotEmpty()) {
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                    shareIntent.type = "*/*"
                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris)
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Share ${fileNames.size} files"
                        )
                    )
                }
            }
            topToolbarCloseOptionFunction()
        }

        //ToolbarRename
        fragmentHomeBinding.cabBottomToolbarHomeFragment.layoutBottomToolbarRename.setOnClickListener {
            val renameSheetDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val renameSheetView = layoutInflater.inflate(R.layout.bottomsheet_rename, null)
            renameSheetDialog.setContentView(renameSheetView)
            renameSheetDialog.setCanceledOnTouchOutside(true)

            val renameEditText = renameSheetView.findViewById<EditText>(R.id.rename_EditText)
            val rename_CancelText = renameSheetView.findViewById<TextView>(R.id.rename_CancelText)
            val rename_OKText = renameSheetView.findViewById<TextView>(R.id.rename_OKText)


            val recordRename = records.filter { it.isChecked }.get(0)

            renameEditText.requestFocus()
            renameEditText.setText(recordRename.fileName)
            val nameWithoutExtension = recordRename.fileName.substring(
                0,
                recordRename.fileName.length - getFileExtension(recordRename.fileName).length
            )
            renameEditText.setSelection(0, nameWithoutExtension.length)
            renameSheetDialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            rename_OKText.setOnClickListener {
                val newFileName = renameEditText.text.toString()
                val file = File(recordRename.filePath)
                val fileExtension = getFileExtension(file.name)
                val newFile = File(file.parent, "$newFileName.$fileExtension")

                if (file.renameTo(newFile)) {
                    GlobalScope.launch {
                        recordRename.fileName = newFileName
                        recordRename.filePath = newFile.absolutePath
                        db.audioRecordDao().update(recordRename)
                        requireActivity().runOnUiThread {
                            homeAdapter.notifyItemChanged(records.indexOf(recordRename))
                        }
                    }
                    renameSheetDialog.dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to rename the file",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


            rename_CancelText.setOnClickListener {
                renameSheetDialog.dismiss()
            }

            topToolbarCloseOptionFunction()
            renameSheetDialog.show()

        }

        //ToolbarDelete
        fragmentHomeBinding.cabBottomToolbarHomeFragment.layoutBottomToolbarDelete.setOnClickListener {
            val deleteSheetDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val deleteSheetView = layoutInflater.inflate(R.layout.bottomsheet_delete, null)
            deleteSheetDialog.setContentView(deleteSheetView)
            deleteSheetDialog.setCanceledOnTouchOutside(true)
            val deleteSelectedText = deleteSheetView.findViewById<TextView>(R.id.deleteSelectedText)
            val deleteDeleteText = deleteSheetView.findViewById<TextView>(R.id.deleteDeleteText)
            val deleteCancelText = deleteSheetView.findViewById<TextView>(R.id.deleteCancelText)

            val recordCounts = records.count() { it.isChecked }
            deleteSelectedText.text = "Delete ${recordCounts} selected items"

            // Delete OK
            deleteDeleteText.setOnClickListener {
                val toDelete = records.filter { it.isChecked }.toTypedArray()
                // Before deletion
                Log.d("Records Before Deletion", records.toString())
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    db.audioRecordDao().delete(toDelete)
                    withContext(Dispatchers.Main) {
                        records.removeAll(toDelete)
                        Log.d("Records After Deletion", records.toString())
                        homeAdapter.notifyDataSetChanged()
                        deleteSheetDialog.dismiss()
                    }
                }
            }


            deleteCancelText.setOnClickListener {
                deleteSheetDialog.dismiss()
            }

            deleteSheetDialog.show()
            topToolbarCloseOptionFunction()
        }

        //ToolbarDetails
        fragmentHomeBinding.cabBottomToolbarHomeFragment.layoutBottomToolbarDetails.setOnClickListener {

            val detailSheetDialog =
                BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
            val detailsSheetView =
                layoutInflater.inflate(R.layout.bottomsheet_details, null)
            detailSheetDialog.setContentView(detailsSheetView)
            detailSheetDialog.setCanceledOnTouchOutside(true)

            val popupDetailsNameText =
                detailsSheetView.findViewById<TextView>(R.id.popupDetailsNameText)
            val popupDetailsPathText =
                detailsSheetView.findViewById<TextView>(R.id.popupDetailsPathText)
            val popupDetailsSizeText =
                detailsSheetView.findViewById<TextView>(R.id.popupDetailsSizeText)
            val popupDetailsLastModifiedText =
                detailsSheetView.findViewById<TextView>(R.id.popupDetailsLastModifiedText)
            val popupDetailsOKText =
                detailsSheetView.findViewById<TextView>(R.id.popupDetailsOKText)


            // Assuming you have a File object representing the selected file
            val selectedFiles = records.filter { it.isChecked }

            for (selectedFile in selectedFiles) {
                // Populate the details in the dialog for each selected file
                popupDetailsNameText.text = "Name: ${selectedFile.fileName}"
                popupDetailsPathText.text = "Path: ${selectedFile.filePath}"

                val fileSizeInBytes: Long = selectedFile.fileSize.toLong()
                popupDetailsSizeText.text = "Size: ${getFormattedFileSize(fileSizeInBytes)}"
                popupDetailsLastModifiedText.text =
                    "Last Modified: ${getFormattedDate(selectedFile.timestamp)}"

            }

            popupDetailsOKText.setOnClickListener {
                detailSheetDialog.dismiss()
            }
            topToolbarCloseOptionFunction()
            detailSheetDialog.show()

            // Helper function to format file size (you can adjust this based on your requirements)


            popupDetailsOKText.setOnClickListener {
                detailSheetDialog.dismiss()
            }
            topToolbarCloseOptionFunction()
            detailSheetDialog.show()
        }


    }

    private fun searchDatabase(query: String) {
        GlobalScope.launch {
            records.clear()
            val retrievedData = db.audioRecordDao().searchDatabase("%$query%")
            records.addAll(retrievedData)

            withContext(Dispatchers.Main) {
                homeAdapter.notifyDataSetChanged()
                fragmentHomeBinding.fileCountHomeFragment.text =
                    "Recording Files : ${homeAdapter.itemCount}"
            }
        }
    }

    override fun onItemClickListener(position: Int) {
        val audioRecord = records[position]

        if (homeAdapter.isEditMode()) {
            records[position].isChecked = !records[position].isChecked
            homeAdapter.notifyItemChanged(position)
            // Check if all items are selected
            val selectedAll = records.all { it.isChecked }
            fragmentHomeBinding.cabTopToolbarInternal.topToolbarSelectedall.setImageResource(
                if (selectedAll) R.drawable.icon_selectall else R.drawable.icon_select_none
            )
            var checkBoxSelected = records.count { it.isChecked }
            when (checkBoxSelected) {
                0 -> {
                    disableDeleteAndSend()
                    disableRenameAndDetails()
                }

                1 -> {
                    enableDeleteAndSend()
                    enableRenameAndDetails()
                }

                else -> {
                    disableRenameAndDetails()
                    enableDeleteAndSend()
                }
            }

        } else {
            val intent = Intent(requireContext(), PlayerActivity::class.java)
            intent.putExtra("filePath", audioRecord.filePath)
            intent.putExtra("fileName", audioRecord.fileName)
            startActivity(intent)
        }
    }

    override fun onItemLongClickListener(position: Int) {
        isLongClickListenerInitialized = true
        fragmentHomeBinding.visibilityBottomToolbar.visibility = View.VISIBLE
        fragmentHomeBinding.visibilityTopToolbar.visibility = View.VISIBLE
        fragmentHomeBinding.visibilitySearchToolbar.visibility = View.GONE
        fragmentHomeBinding.visibilityCardViewHomeFragment.visibility = View.GONE

        homeAdapter.setEditMode(true)
        records[position].isChecked = !records[position].isChecked
        homeAdapter.notifyItemChanged(position)

        enableDeleteAndSend()
        enableRenameAndDetails()

    }


    private fun topToolbarCloseOptionFunction() {
        fragmentHomeBinding.visibilityBottomToolbar.visibility = View.GONE
        fragmentHomeBinding.visibilityTopToolbar.visibility = View.GONE
        fragmentHomeBinding.visibilitySearchToolbar.visibility = View.VISIBLE
        fragmentHomeBinding.visibilityCardViewHomeFragment.visibility = View.VISIBLE

        records.map { it.isChecked = false }
        homeAdapter.setEditMode(false)
    }

    private fun disableRenameAndDetails() {
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarRenameIcon.setImageResource(
            R.drawable.icon_rename_disabled
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDetailsIcon.setImageResource(
            R.drawable.icon_details_disabled
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDetailsIcon.isClickable =
            false
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarRenameIcon.isClickable =
            false
    }

    private fun disableDeleteAndSend() {
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDeleteIcon.setImageResource(
            R.drawable.icon_delete_disabled
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDeleteIcon.isClickable =
            false
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarShareIcon.setImageResource(
            R.drawable.icon_share_disabled
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarShareIcon.isClickable =
            false
    }

    private fun enableRenameAndDetails() {
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarRenameIcon.setImageResource(
            R.drawable.icon_rename
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDetailsIcon.setImageResource(
            R.drawable.icon_details
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDetailsIcon.isClickable =
            true
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarRenameIcon.isClickable =
            true
    }

    private fun enableDeleteAndSend() {
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDeleteIcon.setImageResource(
            R.drawable.icon_delete
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarDeleteIcon.isClickable =
            true
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarShareIcon.setImageResource(
            R.drawable.icon_share
        )
        fragmentHomeBinding.cabBottomToolbarHomeFragment.bottomToolbarShareIcon.isClickable =
            true
    }


    override fun onResume() {
        super.onResume()
        updateRecyclerView()
    }

    private fun updateRecyclerView() {
        GlobalScope.launch {
            val retrievedData = db.audioRecordDao().getAll()
            records.clear()
            records.addAll(retrievedData)
            withContext(Dispatchers.Main) {
                homeAdapter.notifyDataSetChanged()
                fragmentHomeBinding.fileCountHomeFragment.text =
                    "Recording Files : ${homeAdapter.itemCount}"
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

    }


}