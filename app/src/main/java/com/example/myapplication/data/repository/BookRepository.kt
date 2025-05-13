package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import dao.BookDao
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import relations.BookWithSubjects

class BookRepository(private val bookDao: BookDao) {
    val allBooks: LiveData<List<BookEntry>> = bookDao.getAllBooks()

    suspend fun addBook(book: BookEntry) : Long{
        return bookDao.insert(book)
    }

    fun getBookById(bookId: Int): BookEntry {
        return bookDao.getBookById(bookId) ?: throw IllegalArgumentException("Book not found")
    }

   fun isDuplicate(book: BookEntry): Boolean {
        val existingBook = bookDao.getBookByTitle(book.title)
        return existingBook != null
    }
    suspend fun removeBook(book: BookEntry) {
        bookDao.deleteBook(book)
    }

    suspend fun update(book: BookEntry) {
        bookDao.update(book)
    }

    fun getBookWithSubjectsById(bookId: Int): BookWithSubjects? {
        return bookDao.getBookWithSubjectsById(bookId)
    }

    suspend fun insertSubject(subject: Subject) {
//        val newSubject = Subject(subjectName = subject)
        bookDao.insertSubject(subject)
    }

    suspend fun insertCrossRefs(crossRefs: List<BookSubjectCrossRef>) {
        bookDao.insertCrossRefs(crossRefs)
    }
}
