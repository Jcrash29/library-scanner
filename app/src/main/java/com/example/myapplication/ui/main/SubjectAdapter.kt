package com.example.myapplication.ui.main

import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.myapplication.R
import com.example.myapplication.ui.viewmodel.BookViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SubjectsAdapter(private val subjects: MutableList<String>,
                      private val bookViewModel: BookViewModel
) :
    RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectTextView: TextView = itemView.findViewById(R.id.subjectTextView)
        val numberOfBooks: TextView = itemView.findViewById(R.id.numberOfBooksTextView)
        init {
            itemView.setOnClickListener {
                val position = getBindingAdapterPosition()
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(subjects[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.subjectTextView.text = subjects[position]
        holder.numberOfBooks.text = "Loading..."
        // set the numberOfBooks to the number of books associated with the subject
        CoroutineScope(Dispatchers.Main).launch {
            val count = bookViewModel.getBooksCountForSubject(subjects[position])
            println("SubjectsAdapter: Subject ${subjects[position]} has $count books")
            holder.numberOfBooks.text = "$count"
        }
    }

    override fun getItemCount(): Int = subjects.size

    fun removeItem(position: Int) {
        subjects.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getSubjects(): List<String> {
        return subjects
    }

    fun addSubjects(newSubjects: List<String>) {
        subjects.addAll(newSubjects.filter { it !in subjects }) // Avoid duplicates
    }
}
