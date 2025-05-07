package com.example.myapplication.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.model.BookEntry
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityViewBooksBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import androidx.recyclerview.widget.RecyclerView

class ViewBooksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewBooksBinding
    private lateinit var bookAdapter: BookAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityViewBooksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookAdapter = BookAdapter { selectedBook, position ->
            val intent = Intent(this, EditBookActivity::class.java).apply {
                putExtra("book", selectedBook) // Send book data
                putExtra("position", position)
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
                    bookViewModel.removeBook(book) // Remove from database
                    bookAdapter.notifyItemRemoved(position)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    bookAdapter.notifyItemChanged(viewHolder.adapterPosition) // Reset item so it's not removed visually
                }
                .show()
        }
    }
}