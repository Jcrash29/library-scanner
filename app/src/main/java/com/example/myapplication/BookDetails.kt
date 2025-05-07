package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.model.BookEntry
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityBookDetailsBinding
import com.example.myapplication.ui.dialog.DuplicateBookDialog
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookDetailsBinding

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBookDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleTextBox : EditText = findViewById(R.id.titleTextBox);
        val authorTextBox : EditText = findViewById(R.id.authorTextBox);
        val subjectList : ListView = findViewById(R.id.listView);

        /* Extract the parcelable data that was passed in */
        val bookEntry: BookEntry = intent.getParcelableExtra("book")!!
        titleTextBox.setText(bookEntry.title)
        authorTextBox.setText(bookEntry.author)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bookEntry.subjects)

        subjectList.adapter = adapter

        val backButton = findViewById<Button>(R.id.BackToMain)
        backButton.setOnClickListener {
            finish()
        }

        binding.submitBookEntry.setOnClickListener {
            val newBook = BookEntry(title = titleTextBox.text.toString(),
                                    author = authorTextBox.text.toString(),
                                    lccn = bookEntry.lccn,
                                    subjects = bookEntry.subjects,
                                    location = "N/A")

            checkAndAddBook(newBook)
        }
    }

    private fun checkAndAddBook(newBook: BookEntry) {
        lifecycleScope.launch {
            val isDuplicate = bookViewModel.isDuplicate(newBook)
            if (isDuplicate) {
                showDuplicateDialog(newBook)
            } else {
                bookViewModel.addBook(newBook)
                finish()
//                Toast.makeText(this@BookDetails, "Book added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDuplicateDialog(newBook: BookEntry) {
        AlertDialog.Builder(this@BookDetails)
            .setTitle("Duplicate Book")
            .setMessage("A book with the title \"${newBook.title}\" already exists. Do you want to add it anyway?")
            .setPositiveButton("Yes") { _, _ ->
                bookViewModel.addBook(newBook)
                finish()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}