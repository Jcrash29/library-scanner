package com.example.myapplication.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityViewBooksBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.BookDetails
import com.example.myapplication.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.launch

class ViewBooksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewBooksBinding
    private lateinit var bookAdapter: BookAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        println("ViewBooksActivity onCreate called")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityViewBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookAdapter = BookAdapter { selectedBook, position ->
            val intent = Intent(this, BookDetails::class.java).apply {
                putExtra("bookId", selectedBook.bookId)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ViewBooksActivity)
            adapter = bookAdapter
        }

        bookViewModel.allBooks.observe(this) { books ->
            println("ViewBooksActivity Observed books: ${books.size}")
            bookAdapter.submitList(books)
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)

        val addBookButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.addBookButton)
        addBookButton.setOnClickListener {
            println("Add Subject Button Clicked")
            addManualBook()
        }

    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val book = bookAdapter.currentList[viewHolder.adapterPosition] // Get swiped book
            val position = viewHolder.adapterPosition

            AlertDialog.Builder(this@ViewBooksActivity)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete '${book.title}'?")
                .setPositiveButton("Yes") { _, _ ->
                    bookAdapter.notifyItemRemoved(position)
                    lifecycleScope.launch {
                        bookViewModel.removeBook(book) // Remove from database
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    bookAdapter.notifyItemChanged(viewHolder.adapterPosition) // Reset item so it's not removed visually
                }
                .show()
        }
    }

    suspend fun linkSubjectsToBook(bookId: Int, subjects: List<Subject>) {
        val refs = subjects.map { subject ->
            BookSubjectCrossRef(bookId = bookId, subjectName = subject.subjectName)
        }
        bookViewModel.insertCrossRefs(refs)  // Again, use `@Insert(onConflict = IGNORE)`
    }

    private fun addManualBook() {
        //Create a new book with no data, and no subjects
        val newBook = BookEntry(
            title = "",
            author = "",
            lccn = "",
            location = "",
            isbn = "",
            url = ""
        )
        lifecycleScope.launch {
            val bookId = bookViewModel.addBook(newBook).toInt() // Add to database
            val subjects: List<Subject> = emptyList() // No subjects for new book
            subjects.forEach { subject ->
                bookViewModel.insertSubject(subject)
                linkSubjectsToBook(bookId, subjects)
            }

            val intent = Intent(this@ViewBooksActivity, BookDetails::class.java).apply {
                putExtra("bookId", bookId)
            }
            startActivity(intent) // Start BookDetails activity
        }
    }
}