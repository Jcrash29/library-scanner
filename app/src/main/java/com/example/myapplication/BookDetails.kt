package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.database.BookDatabase
import entities.BookEntry
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityBookDetailsBinding
import com.example.myapplication.ui.main.SubjectsAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class BookDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookDetailsBinding

    private lateinit var subjectsRecyclerView: RecyclerView
    private lateinit var subjectsAdapter: SubjectsAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    private var bookEntry: BookEntry? = null // Declare bookEntry as a nullable property

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        println("BookDetails onCreate called")

        binding = ActivityBookDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleTextBox : EditText = findViewById(R.id.titleTextBox);
        val authorTextBox : EditText = findViewById(R.id.authorTextBox);

        // Initialize RecyclerView
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView)
        subjectsRecyclerView.layoutManager = LinearLayoutManager(this)

        /* Extract the parcelable data that was passed in */
        val bookId = intent.getIntExtra("bookId", -1)

        println("About to launch lifecycleScope")
        // Using the bookID, get the book entry from the database
        lifecycleScope.launch {
            val bookWithSubjects = bookViewModel.getBookWithSubjectsById(bookId)

            println("BookWithSubjects: $bookWithSubjects")
            bookWithSubjects?.let {
                val bookEntry = it.book
                val subjects = it.subjects.map { subject -> subject.subjectName } // Extract subject names
                println("subjects: $subjects")
                println("BookDetails: Book ID: ${bookEntry.bookId}, Title: ${bookEntry.title}, Author: ${bookEntry.author}")
                titleTextBox.setText(bookEntry.title)
                authorTextBox.setText(bookEntry.author)

                // Initialize RecyclerView with subjects
                subjectsAdapter = SubjectsAdapter(subjects.toMutableList())
                subjectsRecyclerView.adapter = subjectsAdapter
                println("BookDetails: Subjects: ${subjects.joinToString(", ")}")
            }
        }

        val backButton = findViewById<Button>(R.id.saveBook)
        backButton.setOnClickListener {
            //create list of subjects that are in the recycler view
            val subjects = subjectsAdapter.getSubjects()

            // Update the book entry with the new title, author
            lifecycleScope.launch {
                bookViewModel.updateBook(){
                    bookEntry.copy?(
                        title = titleTextBox.text.toString(),
                        author = authorTextBox.text.toString(),

                    )
                }
            }


            finish()
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.subjectsRecyclerView)

        binding.deleteBook.setOnClickListener {
            lifecycleScope.launch {
                val confirm = confirmDelete()
                if (confirm) {
                    val bookEntry = bookViewModel.getBookById(bookId)
                    bookViewModel.removeBook(bookEntry)
                    finish()
                }
            }
        }
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            subjectsAdapter.removeItem(position)
        }
    }

    private suspend fun confirmDelete(): Boolean {
        return suspendCancellableCoroutine { cont ->
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete Book")
                .setMessage("Are you sure you wish to delete this book?")
                .setPositiveButton("Yes") { _, _ ->
                    cont.resume(true)
                }
                .setNegativeButton("No") { dialog, _ ->
                    cont.resume(false)
                }
                .show()
        }
    }
}