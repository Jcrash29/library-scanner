package com.example.myapplication.ui

import android.R
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.model.BookEntry
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityEditBookBinding
import com.example.myapplication.databinding.ActivityViewBooksBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory

class EditBookActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBookBinding
    private lateinit var bookViewModel: BookViewModel
    private var currentBook: BookEntry? = null
    private lateinit var bookAdapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repository = BookRepository(BookDatabase.getDatabase(this).bookDao())
        val factory = BookViewModelFactory(repository)
        bookViewModel = ViewModelProvider(this, factory).get(BookViewModel::class.java)

        // 🔹 Get book data from Intent
        currentBook = intent.getParcelableExtra("book")
        currentBook?.let { book ->
            binding.titleEditText.setText(book.title)
            binding.authorEditText.setText(book.author)
        }

        val adapter = currentBook?.let { ArrayAdapter(this, R.layout.simple_list_item_1, it.subjects) }

        binding.listView.adapter = adapter

        // Get book position
        val position: Int = intent.getIntExtra("position", -1)

        bookAdapter = BookAdapter { selectedBook, position ->
            val intent = Intent(this, EditBookActivity::class.java).apply {
                intent.putExtra("book", selectedBook) // Send book data
                intent.putExtra("position", position) // Send position
            }
            startActivity(intent)
        }

        binding.saveButton.setOnClickListener {
            val updatedBook = currentBook?.copy(
                title = binding.titleEditText.text.toString(),
                author = binding.authorEditText.text.toString()
            )

            if (updatedBook != null) {
                bookViewModel.updateBook(updatedBook)
            }

            val resultIntent = Intent().apply {
                putExtra("updatedBook", updatedBook)
                putExtra("position", position)
            }
            setResult(Activity.RESULT_OK, resultIntent)

            finish() // 🔹 Close activity after saving
        }
    }
}
