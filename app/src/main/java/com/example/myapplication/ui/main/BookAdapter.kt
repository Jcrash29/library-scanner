package com.example.myapplication.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import entities.BookEntry

//class BookAdapter : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {
class BookAdapter(private val onItemClick: (BookEntry, Int) -> Unit) :
    ListAdapter<BookEntry, BookAdapter.BookViewHolder>(BookDiffUtil()) {
    class BookViewHolder(itemView: View, private val onItemClick: (BookEntry, Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.bookTitle)
        private val authorTextView: TextView = itemView.findViewById(R.id.bookAuthor)

        fun bind(book: BookEntry, position: Int) {
            titleTextView.text = book.title
            authorTextView.text = book.author
            itemView.setOnClickListener { onItemClick(book, position) }
        }
    }

    class BookDiffUtil: DiffUtil.ItemCallback<BookEntry>() {
        override fun areItemsTheSame(oldItem: BookEntry, newItem: BookEntry): Boolean {
            /* TODO: Currently only checking for duplicate title */
            return oldItem.title == newItem.title && oldItem.author == newItem.author
        }

        override fun areContentsTheSame(oldItem: BookEntry, newItem: BookEntry): Boolean {
            return areItemsTheSame(oldItem, newItem)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        println("BookAdapter Binding book: ${book.title}")
        holder.bind(book, position)
    }
}
