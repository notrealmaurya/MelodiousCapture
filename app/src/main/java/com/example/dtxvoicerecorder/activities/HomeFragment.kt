package com.example.dtxvoicerecorder.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.dtxvoicerecorder.R
import com.example.dtxvoicerecorder.database.AppDatabase
import com.example.dtxvoicerecorder.database.RecorderData
import com.example.dtxvoicerecorder.database.RecorderFilesAdapter
import com.example.dtxvoicerecorder.databinding.FragmentHomeBinding
import com.example.dtxvoicerecorder.databinding.PopupAboutDialogBinding
import com.example.dtxvoicerecorder.utils.OnItemClickListener
import com.example.dtxvoicerecorder.utils.SharedPreferenceHelper
import com.example.dtxvoicerecorder.utils.getFileExtension
import com.example.dtxvoicerecorder.utils.getFormattedDate
import com.example.dtxvoicerecorder.utils.getFormattedFileSize
import com.example.dtxvoicerecorder.utils.isExternalStorageWritable
import com.example.dtxvoicerecorder.utils.isFilePresent
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), OnItemClickListener {

    private lateinit var fragmentHomeBinding: FragmentHomeBinding
    private lateinit var records: ArrayList<RecorderData>
    private lateinit var db: AppDatabase
    private var isAllChecked = false
    private var isLongClickListenerInitialized = false
    private var filePath: String = ""
    private val themeList = arrayOf("Light Mode", "Dark Mode", "Auto")

    private lateinit var navController: NavController


    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferenceHelper

    companion object {
        lateinit var homeAdapter: RecorderFilesAdapter
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = fragmentHomeBinding.root

        sharedPreferencesHelper = SharedPreferenceHelper((requireContext()))


        records = ArrayList()
        db = Room.databaseBuilder(requireContext(), AppDatabase::class.java, "audioRecords").build()

        fragmentHomeBinding.recyclerViewHomeFragment.setHasFixedSize(true)
        fragmentHomeBinding.recyclerViewHomeFragment.setItemViewCacheSize(13)
        fragmentHomeBinding.recyclerViewHomeFragment.layoutManager = LinearLayoutManager(context)
        homeAdapter = RecorderFilesAdapter(requireContext(), this, records)
        fragmentHomeBinding.recyclerViewHomeFragment.adapter = homeAdapter



        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
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


        fragmentHomeBinding.visibilityTopToolbar.visibility = View.GONE
        fragmentHomeBinding.visibilityBottomToolbar.visibility = View.GONE
        fragmentHomeBinding.visibilityCardViewHomeFragment.visibility = View.VISIBLE

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
            navController.navigate(R.id.recorderActivity)
        }

        //Toolbar CLose
        fragmentHomeBinding.topToolbarClose.setOnClickListener {
            topToolbarCloseOptionFunction()
        }

        //Toolbar SelectAll
        fragmentHomeBinding.topToolbarSelectedall.setOnClickListener {
            isAllChecked = !isAllChecked
            records.map { it.isChecked = isAllChecked }
            homeAdapter.notifyDataSetChanged()

            if (isAllChecked) {
                enableDeleteAndSend()
            } else {
                disableDeleteAndSend()
            }
            disableRenameAndDetails()
            fragmentHomeBinding.topToolbarSelectedall.setImageResource(
                if (isAllChecked) R.drawable.icon_selectall else R.drawable.icon_select_none
            )

        }

        //ToolbarShare
        fragmentHomeBinding.layoutBottomToolbarShare.setOnClickListener {
            if (records.count { it.isChecked } >= 1) {
                val selectedFiles = records.filter { it.isChecked }


                if (selectedFiles.isNotEmpty()) {
                    val fileUris = ArrayList<Uri>()
                    val fileNames = ArrayList<String>()

                    for (selectedFile in selectedFiles) {
                        val file = File(selectedFile.filePath)
                        val fileUri = FileProvider.getUriForFile(
                            requireContext(), "${requireContext().packageName}.provider", file
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
                                shareIntent, "Share ${fileNames.size} files"
                            )
                        )
                    }
                }
                topToolbarCloseOptionFunction()
            } else {
                disableDeleteAndSend()
            }
        }

        //ToolbarRename
        fragmentHomeBinding.layoutBottomToolbarRename.setOnClickListener {

            if (records.count { it.isChecked } == 1) {
                val renameSheetDialog =
                    BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
                val renameSheetView = layoutInflater.inflate(R.layout.bottomsheet_rename, null)
                renameSheetDialog.setContentView(renameSheetView)
                renameSheetDialog.setCanceledOnTouchOutside(true)

                val renameEditText = renameSheetView.findViewById<EditText>(R.id.rename_EditText)
                val rename_CancelText =
                    renameSheetView.findViewById<TextView>(R.id.rename_CancelText)
                val rename_OKText = renameSheetView.findViewById<TextView>(R.id.rename_OKText)


                val recordRename = records.filter { it.isChecked }[0]

                renameEditText.requestFocus()
                renameEditText.setText(recordRename.fileName)
                val nameWithoutExtension = recordRename.fileName.substring(
                    0, recordRename.fileName.length - getFileExtension(recordRename.fileName).length
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
                            requireContext(), "Failed to rename the file", Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                rename_CancelText.setOnClickListener {
                    renameSheetDialog.dismiss()
                }

                topToolbarCloseOptionFunction()
                renameSheetDialog.show()

            } else {
                disableRenameAndDetails()
            }
        }

        //ToolbarDelete
        fragmentHomeBinding.layoutBottomToolbarDelete.setOnClickListener {

            if (records.count { it.isChecked } >= 1) {
                val deleteSheetDialog =
                    BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
                val deleteSheetView = layoutInflater.inflate(R.layout.bottomsheet_delete, null)
                deleteSheetDialog.setContentView(deleteSheetView)
                deleteSheetDialog.setCanceledOnTouchOutside(true)
                val deleteSelectedText =
                    deleteSheetView.findViewById<TextView>(R.id.deleteSelectedText)
                val deleteDeleteText = deleteSheetView.findViewById<TextView>(R.id.deleteDeleteText)
                val deleteCancelText = deleteSheetView.findViewById<TextView>(R.id.deleteCancelText)

                val itemsToRemove = records.filter { it.isChecked }
                val recordCounts = itemsToRemove.size
                deleteSelectedText.text = "Delete $recordCounts selected items"

                // Delete OK
                deleteDeleteText.setOnClickListener {
                    GlobalScope.launch {
                        for (item in itemsToRemove) {
                            db.audioRecordDao().delete(item)
                        }

                        withContext(Dispatchers.Main) {
                            records.removeAll(itemsToRemove)
                            homeAdapter.notifyDataSetChanged()
                        }
                    }
                    updateRecyclerView()
                    deleteSheetDialog.dismiss()
                }

                deleteCancelText.setOnClickListener {
                    deleteSheetDialog.dismiss()
                }

                deleteSheetDialog.show()
                topToolbarCloseOptionFunction()
            } else {
                disableDeleteAndSend()
            }
        }


        //ToolbarDetails
        fragmentHomeBinding.layoutBottomToolbarDetails.setOnClickListener {

            if (records.count { it.isChecked } == 1) {
                val detailSheetDialog =
                    BottomSheetDialog(requireContext(), R.style.ThemeOverlay_App_BottomSheetDialog)
                val detailsSheetView = layoutInflater.inflate(R.layout.bottomsheet_details, null)
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
            } else {
                disableRenameAndDetails()
            }

        }


        //menu
        fragmentHomeBinding.menuHomeFragment.setOnClickListener {

            val inflater =
                requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.menu_layout, null)

            val popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
            )
            popupWindow.showAtLocation(fragmentHomeBinding.menuHomeFragment, Gravity.TOP, 150, 400)

            val share = popupView.findViewById<LinearLayout>(R.id.shareLayoutPopUp)
            val feedback = popupView.findViewById<LinearLayout>(R.id.feedbackLayoutPopUp)
            val about = popupView.findViewById<LinearLayout>(R.id.aboutLayoutPopUp)
            val exit = popupView.findViewById<LinearLayout>(R.id.exitLayoutPopUp)
            val themeText = popupView.findViewById<TextView>(R.id.themeTextLayoutPopUp)
            val theme = popupView.findViewById<LinearLayout>(R.id.themeLayoutPopUp)

            var checkedTheme = sharedPreferencesHelper.theme
            themeText.text = themeList[sharedPreferencesHelper.theme]

            share.setOnClickListener {
                popupWindow.dismiss()
                val websiteUrl =
                    "https://github.com/notrealmaurya"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                startActivity(intent)
            }

            theme.setOnClickListener {
                popupWindow.dismiss()
                val dialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Change theme")
                    .setPositiveButton("Ok") { _, _ ->
                        sharedPreferencesHelper.theme = checkedTheme
                        AppCompatDelegate.setDefaultNightMode(sharedPreferencesHelper.themeFlag[checkedTheme])
                        themeText.text = themeList[sharedPreferencesHelper.theme]
                    }
                    .setSingleChoiceItems(themeList, checkedTheme) { _, which ->
                        checkedTheme = which
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
                dialog.setOnDismissListener {
                    dialog.dismiss()
                }


            }

            feedback.setOnClickListener {
                popupWindow.dismiss()
                val websiteUrl =
                    "https://forms.gle/4gC2XzHDCaio7hUh8"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                startActivity(intent)
            }

            about.setOnClickListener {
                val popUpDialog = LayoutInflater.from(requireContext())
                    .inflate(R.layout.popup_about_dialog, fragmentHomeBinding.root, false)
                val bindingPopUp = PopupAboutDialogBinding.bind(popUpDialog)
                val dialog =
                    MaterialAlertDialogBuilder(requireContext(), R.style.PopUpWindowStyle).setView(
                        popUpDialog
                    )
                        .setOnCancelListener {

                        }
                        .create()

                bindingPopUp.aboutDialogThankyouButton.setOnClickListener {
                    dialog.dismiss()
                }

                val textView = bindingPopUp.spannableTextViewDialog
                val spannableString =
                    SpannableString("If you'd like to share your thoughts or provide Feedback , please feel free to do so. Your input is valuable, and I'd appreciate hearing from you.❤\uFE0F\"\n ")

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        val websiteUrl =
                            "https://forms.gle/4gC2XzHDCaio7hUh8"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
                        startActivity(intent)
                    }
                }

                spannableString.setSpan(
                    clickableSpan,
                    48, 56,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val blueColor = Color.BLUE
                spannableString.setSpan(
                    ForegroundColorSpan(blueColor),
                    48, 56,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                textView.text = spannableString
                textView.movementMethod = LinkMovementMethod.getInstance()

                dialog.show()

            }

            exit.setOnClickListener {
                popupWindow.dismiss()
                requireActivity().finish()
            }


        }

    }

    override fun onItemCheckedChange(position: Int, isChecked: Boolean) {
        val selectedCount = records.count { it.isChecked }
        fragmentHomeBinding.topToolbarSelectedtext.text = "$selectedCount Selected"
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
            fragmentHomeBinding.topToolbarSelectedall.setImageResource(
                if (selectedAll) R.drawable.icon_selectall else R.drawable.icon_select_none
            )

            val checkBoxSelected = records.count { it.isChecked }
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
        fragmentHomeBinding.bottomToolbarRenameIcon.setImageResource(
            R.drawable.icon_rename_disabled
        )
        fragmentHomeBinding.bottomToolbarDetailsIcon.setImageResource(
            R.drawable.icon_details_disabled
        )
        fragmentHomeBinding.bottomToolbarDetailsIcon.isClickable = false
        fragmentHomeBinding.bottomToolbarRenameIcon.isClickable = false
    }

    private fun disableDeleteAndSend() {
        fragmentHomeBinding.bottomToolbarDeleteIcon.setImageResource(
            R.drawable.icon_delete_disabled
        )
        fragmentHomeBinding.bottomToolbarDeleteIcon.isClickable = false
        fragmentHomeBinding.bottomToolbarShareIcon.setImageResource(
            R.drawable.icon_share_disabled
        )
        fragmentHomeBinding.bottomToolbarShareIcon.isClickable = false
    }

    private fun enableRenameAndDetails() {
        fragmentHomeBinding.bottomToolbarRenameIcon.setImageResource(
            R.drawable.icon_rename
        )
        fragmentHomeBinding.bottomToolbarDetailsIcon.setImageResource(
            R.drawable.icon_details
        )
        fragmentHomeBinding.bottomToolbarDetailsIcon.isClickable = true
        fragmentHomeBinding.bottomToolbarRenameIcon.isClickable = true
    }

    private fun enableDeleteAndSend() {
        fragmentHomeBinding.bottomToolbarDeleteIcon.setImageResource(
            R.drawable.icon_delete
        )
        fragmentHomeBinding.bottomToolbarDeleteIcon.isClickable = true
        fragmentHomeBinding.bottomToolbarShareIcon.setImageResource(
            R.drawable.icon_share
        )
        fragmentHomeBinding.bottomToolbarShareIcon.isClickable = true
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
    }

}