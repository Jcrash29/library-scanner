package dao

import entities.BookEntry
import androidx.lifecycle.LiveData
import androidx.room.*
import entities.BookSubjectCrossRef
import entities.Subject
import relations.BookWithSubjects

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntry): Long

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): LiveData<List<BookEntry>>

    @Query("SELECT * FROM books WHERE title = :title LIMIT 1")
    fun getBookByTitle(title: String): BookEntry?

    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    fun getBookById(bookId: Int): BookEntry?

    @Delete
    suspend fun deleteBook(book: BookEntry)

    @Update
    suspend fun update(book: BookEntry)

    @Transaction
    @Query("SELECT * FROM books")
    fun getBooksWithSubjects(): LiveData<List<BookWithSubjects>>

    @Transaction
    @Query("SELECT * FROM books WHERE bookId = :bookId LIMIT 1")
    fun getBookWithSubjectsById(bookId: Int): BookWithSubjects?

    @Transaction
    @Query("SELECT * FROM books WHERE title = :title LIMIT 1")
    fun getBookWithSubjects(title: String): BookWithSubjects?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubject(subject: Subject)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSubjects(subjects: List<Subject>)

    // CrossRef operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<BookSubjectCrossRef>)
}