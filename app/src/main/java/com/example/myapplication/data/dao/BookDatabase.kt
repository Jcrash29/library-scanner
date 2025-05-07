package com.example.myapplication.data.dao

import com.example.myapplication.data.model.BookEntry
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntry)

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllBooks(): LiveData<List<BookEntry>>

    @Query("SELECT * FROM books WHERE title = :title LIMIT 1")
    fun getBookByTitle(title: String): BookEntry?

    @Delete
    suspend fun deleteBook(book: BookEntry)

    @Update
    suspend fun update(book: BookEntry)
}