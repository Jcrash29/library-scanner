package com.example.myapplication.ui

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityViewBooksBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import entities.BookSubjectCrossRef
import entities.Subject

class ViewBooksActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewBooksBinding
    private lateinit var bookAdapter: BookAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

}