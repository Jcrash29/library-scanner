package com.example.myapplication.ui.viewmodel

import com.example.myapplication.data.model.BookEntry
import com.example.myapplication.data.repository.BookRepository
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookViewModel(private val repository: BookRepository) : ViewModel() {
    val allBooks: LiveData<List<BookEntry>> = repository.allBooks

    init {
        allBooks.observeForever { books ->
            println("BookViewModel Books in DB: ${books.size}") // Check if data exists
        }
    }
    fun addBook(book: BookEntry) = viewModelScope.launch {
        repository.addBook(book)
        println("BookViewModel Inserted book: ${book.title}")
    }

    suspend fun isDuplicate(newBook: BookEntry): Boolean {
        return withContext(Dispatchers.IO) {
            repository.isDuplicate(newBook)
        }
    }

    fun removeBook(book: BookEntry) = viewModelScope.launch {
        repository.removeBook(book)
    }

    fun updateBook(book: BookEntry) = viewModelScope.launch {
        repository.update(book)
    }
}