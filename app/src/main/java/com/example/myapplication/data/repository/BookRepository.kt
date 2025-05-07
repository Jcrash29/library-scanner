package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import com.example.myapplication.data.dao.BookDao
import com.example.myapplication.data.model.BookEntry

class BookRepository(private val bookDao: BookDao) {
    val allBooks: LiveData<List<BookEntry>> = bookDao.getAllBooks()

    suspend fun addBook(book: BookEntry) {
        bookDao.insert(book)
    }

   suspend fun isDuplicate(book: BookEntry): Boolean {
        val existingBook = bookDao.getBookByTitle(book.title)
        return existingBook != null
    }
    suspend fun removeBook(book: BookEntry) {
        bookDao.deleteBook(book)
    }

    suspend fun update(book: BookEntry) {
        bookDao.update(book)
    }
}
