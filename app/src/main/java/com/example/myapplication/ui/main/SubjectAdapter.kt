package com.example.myapplication.ui.main

import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.myapplication.R


class SubjectsAdapter(private val subjects: MutableList<String>) :
    RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectTextView: TextView = itemView.findViewById(R.id.subjectTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        holder.subjectTextView.text = subjects[position]
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
