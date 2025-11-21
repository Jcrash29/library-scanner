package com.example.myapplication

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.database.BookDatabase
import entities.BookEntry
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityBookDetailsBinding
import com.example.myapplication.ui.main.SubjectsAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class BookDetails : AppCompatActivity() {
    private lateinit var binding: ActivityBookDetailsBinding

    private lateinit var subjectsRecyclerView: RecyclerView
    private lateinit var subjectsAdapter: SubjectsAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    private var bookEntry: BookEntry? = null // Declare bookEntry as a nullable property

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        println("BookDetails onCreate called")

        binding = ActivityBookDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val titleTextBox: EditText = findViewById(R.id.titleTextBox);
        val authorTextBox: EditText = findViewById(R.id.authorTextBox);

        // Initialize RecyclerView
        subjectsRecyclerView = findViewById(R.id.subjectsRecyclerView)
        subjectsRecyclerView.layoutManager = LinearLayoutManager(this)

        /* Extract the parcelable data that was passed in */
        val bookId = intent.getIntExtra("bookId", -1)

        if (bookId == -1) {
            println("Book ID is -1, returning")
            finish()
            return
        }

        println("About to launch lifecycleScope")
        // Using the bookID, get the book entry from the database
        lifecycleScope.launch {
            val bookWithSubjects = bookViewModel.getBookWithSubjectsById(bookId)

            println("BookWithSubjects: $bookWithSubjects")
            bookWithSubjects?.let {
                bookEntry = it.book
                val subjects = it.subjects.map { subject -> subject.subjectName }
                println("subjects: $subjects")
                println("BookDetails: Book ID: ${bookEntry?.bookId}, Title: ${bookEntry?.title}, Author: ${bookEntry?.author}")
                titleTextBox.setText(bookEntry?.title)
                authorTextBox.setText(bookEntry?.author)

                // Initialize RecyclerView adapter only once
                if (!::subjectsAdapter.isInitialized) {
                    subjectsAdapter = SubjectsAdapter(mutableListOf(), bookViewModel)
                    subjectsRecyclerView.adapter = subjectsAdapter
                }

                // Update the adapter's data after loading
                subjectsAdapter.addSubjects(subjects)
                subjectsAdapter.notifyDataSetChanged()
                println("Loading BookDetails: Subjects: ${subjects.joinToString(", ")}")
            }
        }

        val saveButton = findViewById<Button>(R.id.saveBook)
        saveButton.setOnClickListener {
            val bookEntryUpdated = bookEntry?.copy(
                title = titleTextBox.text.toString(),
                author = authorTextBox.text.toString()
            )
            //create list of subjects that are in the recycler view
            val subjects = subjectsAdapter.getSubjects()
            println("Saving BookEntry: Subjects: ${subjects.joinToString(", ")}")

            // Update the book entry with the new title, author
            lifecycleScope.launch {
                if (bookEntryUpdated != null) {
                    bookViewModel.updateBook(bookEntryUpdated)

                    // Step 1: Clear existing cross-references for the book
                    bookEntryUpdated.let { book ->
                        bookViewModel.clearBookSubjects(book.bookId)

                        // Step 2: Create new cross-references for the active subjects
                        val crossRefs = subjects.map { subjectName ->
                            BookSubjectCrossRef(bookId = book.bookId, subjectName = subjectName)
                        }

                        // Step 3: Insert the new cross-references into the database
                        bookViewModel.insertCrossRefs(crossRefs)
                    }
                }
            }
            finish()
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.subjectsRecyclerView)

        val addSubjectButton: FloatingActionButton = findViewById<FloatingActionButton>(R.id.addSubjectButton)
        addSubjectButton.setOnClickListener {
            println("Add Subject Button Clicked")
            showSelectSubjectDialog()
        }

        binding.deleteBook.setOnClickListener {
            lifecycleScope.launch {
                val confirm = confirmDelete()
                if (confirm) {
                    val bookEntry = bookViewModel.getBookById(bookId)
                    bookViewModel.removeBook(bookEntry)
                    finish()
                }
            }
        }
    }

    private val itemTouchHelperCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                subjectsAdapter.removeItem(position)
            }
        }

    private suspend fun confirmDelete(): Boolean {
        return suspendCancellableCoroutine { cont ->
            AlertDialog.Builder(this)
                .setTitle("Confirm Delete Book")
                .setMessage("Are you sure you wish to delete this book?")
                .setPositiveButton("Yes") { _, _ ->
                    cont.resume(true)
                }
                .setNegativeButton("No") { dialog, _ ->
                    cont.resume(false)
                }
                .show()
        }
    }

    private fun showSelectSubjectDialog() {
        lifecycleScope.launch {
            // Fetch existing subjects from the database
            val existingSubjects = bookViewModel.getAllSubjects()

            // Convert to an array for the dialog
            val subjectNames = existingSubjects.map { it.subjectName }.toMutableList()
            val selectedSubjects = MutableList(subjectNames.size) { false }

            // Create a custom layout with a scrollable ListView
            val listView = ListView(this@BookDetails).apply {
                adapter = ArrayAdapter(
                    this@BookDetails,
                    android.R.layout.simple_list_item_multiple_choice,
                    subjectNames
                )
                choiceMode = ListView.CHOICE_MODE_MULTIPLE
                for (i in selectedSubjects.indices) {
                    setItemChecked(i, selectedSubjects[i])
                }
                setOnItemClickListener { _, _, position, _ ->
                    selectedSubjects[position] = !selectedSubjects[position]
                }
            }

            AlertDialog.Builder(this@BookDetails)
                .setTitle("Select or Add Subjects")
                .setView(listView) // Set the scrollable ListView as the dialog content
                .setPositiveButton("Add Selected") { _, _ ->
                    // Add selected subjects to the adapter
                    val newSubjects = subjectNames.filterIndexed { index, _ -> selectedSubjects[index] }
                    val newSubjectEntities = newSubjects.map { subjectName -> Subject(subjectName = subjectName) }
                    lifecycleScope.launch {
                        newSubjectEntities.forEach { subject ->
                            bookViewModel.insertSubject(subject)
                        }
                    }
                    subjectsAdapter.addSubjects(newSubjects)
                    subjectsAdapter.notifyDataSetChanged()
                }
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Add Custom") { _, _ ->
                    showAddCustomSubjectDialog(subjectNames, selectedSubjects)
                }
                .show()
        }
    }
    private fun showAddCustomSubjectDialog(subjectNames: MutableList<String>, selectedSubjects: MutableList<Boolean>) {
        val input = EditText(this)
        input.hint = "Enter new subject"

        AlertDialog.Builder(this)
            .setTitle("Add Custom Subject")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val newSubject = input.text.toString().trim()
                if (newSubject.isNotEmpty() && newSubject !in subjectNames) {
                    lifecycleScope.launch {
                        val newSubjectSubject = Subject(subjectName = newSubject)
                        bookViewModel.insertSubject(newSubjectSubject)
                    }
                    subjectNames.add(newSubject)
                    selectedSubjects.add(false) // Add a new element to the list
                    subjectsAdapter.addSubjects(listOf(newSubject))
                    subjectsAdapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        // Custom logic before the activity is closed
        println("Back button pressed")

        if(bookEntry == null)
        {
            super.onBackPressed()
        }
        // Check if the title is empty, if it is, we delete the book from the database
        if (bookEntry?.title?.isEmpty() == true) {
            lifecycleScope.launch {
                val bookEntry = bookViewModel.getBookById(bookEntry?.bookId ?: -1)
                bookViewModel.removeBook(bookEntry)
            }
        }
        // Call the superclass method to handle the default behavior
        super.onBackPressed()
    }
}
