package com.example.myapplication.ui.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class DuplicateBookDialog(
    private val title: String,
    private val onPositiveClick: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Duplicate Book")
            .setMessage("A book with the title \"$title\" already exists. Do you want to add it anyway?")
            .setPositiveButton("Yes") { _, _ ->
                onPositiveClick()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
    }
}
