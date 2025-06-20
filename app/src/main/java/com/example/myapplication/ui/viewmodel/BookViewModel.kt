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
    val allBooks: LiveData<List<BookWithSubjects>> = repository.allBooksWithSubjects
    var filteredBooks: LiveData<List<BookWithSubjects>> = allBooks

    init {
        viewModelScope.launch {
        }
    }

    fun filterBooks(selectedSubjects: List<String>) {
        filteredBooks = if (selectedSubjects.isEmpty()) {
            allBooks
        } else {
            allBooks.switchMap() { books ->
                books.filter {
                    it.subjects.any { subject ->
                        selectedSubjects.contains(subject.subjectName)
                    }
                }.let {
                    MutableLiveData(it)
                }
            }
        }
    }

    suspend fun addBook(book: BookEntry): Long {
        println("BookViewModel Inserted book: ${book.title}")
        return repository.addBook(book)
    }

    suspend fun isDuplicate(newBook: BookEntry): Boolean {
        return withContext(Dispatchers.IO) {
            repository.isDuplicate(newBook)
        }
    }

    suspend fun getBookById(bookId: Int): BookEntry {
        return repository.getBookById(bookId)
    }

    suspend fun getBookWithSubjectsById(bookId: Int): BookWithSubjects? {
        return repository.getBookWithSubjectsById(bookId)
    }

    suspend fun removeBook(book: BookEntry) = viewModelScope.launch {
        println("BookViewModel: Removing book: ${book.title}")

        // Get the subjects associated with the book
        val bookWithSubjects = repository.getBookWithSubjectsById(book.bookId)
        val associatedSubjects = bookWithSubjects?.subjects ?: emptyList()

        // Remove the book from the repository
        repository.removeBook(book)

        // Check if each subject is still associated with any other books
        for (subject in associatedSubjects) {
            val isSubjectUsed = withContext(Dispatchers.IO) {
                repository.getBooksWithSubject(subject.subjectName).isNotEmpty()
            }
            // If the subject is not used by any other book, remove it
            if (!isSubjectUsed) {
                println("BookViewModel: Removing unused subject: ${subject.subjectName}")
                repository.removeSubject(subject)
            }
            else
            {
                println("BookViewModel: Subject ${subject.subjectName} is still used by other books, not removing.")
            }
        }
    }

    suspend fun updateBook(book: BookEntry) = viewModelScope.launch {
        repository.update(book)
    }

    suspend fun insertSubject(subject: Subject) = viewModelScope.launch {
        repository.insertSubject(subject)
    }

    suspend fun insertCrossRefs(crossRefs: List<BookSubjectCrossRef>) = viewModelScope.launch {
        repository.insertCrossRefs(crossRefs)
    }

    suspend fun clearBookSubjects(bookId: Int) = viewModelScope.launch {
        repository.clearBookSubjects(bookId)
    }

    suspend fun getAllSubjects(): List<Subject> {
        return repository.getAllSubjects()
    }

suspend fun getBooksCountForSubject(subjectName: String): Int {
        return withContext(Dispatchers.IO) {
            repository.getBooksWithSubject(subjectName).size
        }
    }
}