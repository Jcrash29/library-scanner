package com.example.myapplication.ui.viewmodel

import entities.BookEntry
import com.example.myapplication.data.repository.BookRepository
import androidx.lifecycle.*
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import relations.BookWithSubjects

class BookViewModel(private val repository: BookRepository) : ViewModel() {
    val allBooks: LiveData<List<BookEntry>> = repository.allBooks

    init {
        allBooks.observeForever { books ->
            println("BookViewModel Books in DB: ${books.size}") // Check if data exists
        }
    }
    suspend fun addBook(book: BookEntry) : Long{
        println("BookViewModel Inserted book: ${book.title}")
        return repository.addBook(book)

    }

    suspend fun isDuplicate(newBook: BookEntry): Boolean {
        return withContext(Dispatchers.IO) {
            repository.isDuplicate(newBook)
        }
    }

    fun getBookById(bookId: Int): BookEntry {
        return repository.getBookById(bookId)
    }

    fun getBookWithSubjectsById(bookId: Int): BookWithSubjects? {
        return repository.getBookWithSubjectsById(bookId)
    }

    fun removeBook(book: BookEntry) = viewModelScope.launch {
        repository.removeBook(book)
    }

    fun updateBook(book: BookEntry) = viewModelScope.launch {
        repository.update(book)
    }

    fun insertSubject(subject: Subject) = viewModelScope.launch {
        repository.insertSubject(subject)
    }

    fun insertCrossRefs(crossRefs: List<BookSubjectCrossRef>) = viewModelScope.launch {
        repository.insertCrossRefs(crossRefs)
    }
}