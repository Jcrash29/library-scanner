package com.example.myapplication.data.repository

import androidx.lifecycle.LiveData
import dao.BookDao
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import relations.BookWithSubjects

class BookRepository(private val bookDao: BookDao) {
    val allBooks: LiveData<List<BookEntry>> = bookDao.getAllBooks()

    suspend fun addBook(book: BookEntry): Long =
        withContext(Dispatchers.IO) {
            bookDao.insert(book)
        }

    suspend fun getBookById(bookId: Int): BookEntry =
        withContext(Dispatchers.IO) {
            bookDao.getBookById(bookId)
        } ?: throw IllegalArgumentException("Book not found")

   suspend fun isDuplicate(book: BookEntry): Boolean =
        withContext(Dispatchers.IO) {
            val existingBook = bookDao.getBookByTitle(book.title)
            existingBook != null
        }
    suspend fun removeBook(book: BookEntry) =
        withContext(Dispatchers.IO) {
            bookDao.deleteBook(book)
        }

    suspend fun update(book: BookEntry) =
        withContext(Dispatchers.IO) {
            bookDao.update(book)
        }

    suspend fun getBookWithSubjectsById(bookId: Int): BookWithSubjects? =
        withContext(Dispatchers.IO) {
            bookDao.getBookWithSubjectsById(bookId)
        } ?: throw IllegalArgumentException("Book with ID $bookId not found")

    suspend fun insertSubject(subject: Subject) =
        withContext(Dispatchers.IO) {
            bookDao.insertSubject(subject)
        }

    suspend fun insertCrossRefs(crossRefs: List<BookSubjectCrossRef>) =
        withContext(Dispatchers.IO) {
            bookDao.insertCrossRefs(crossRefs)
        }

    suspend fun clearBookSubjects(bookId: Int) =
        withContext(Dispatchers.IO) {
            bookDao.clearBookSubjects(bookId)
        }

    suspend fun getAllSubjects(): List<Subject> =
        withContext(Dispatchers.IO) {
            bookDao.getAllSubjects()
    }
}
