package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.BookDetails
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var bookAdapter: BookAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(requireContext()).bookDao()))
    }

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get the bookId from the intent extras, if available
        val filterPassedIn = arguments?.getString("filter") ?: ""
        if (filterPassedIn.isNotEmpty()) {
            println("HomeFragment: Filter passed in: $filterPassedIn")
            bookViewModel.filterBooks(listOf(filterPassedIn))
            updateFilteredBooks()
        }

        bookAdapter = BookAdapter { selectedBook, position ->
            val intent = Intent(requireContext(), BookDetails::class.java).apply {
                putExtra("bookId", selectedBook.bookId)
            }
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookAdapter
        }

        bookViewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            println("HomeFragment Observed books: ${books.size}")
            val bookEntry = books.map { it.book }
            bookAdapter.submitList(bookEntry)
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)

        binding.addBookButton.setOnClickListener {
            println("Add Subject Button Clicked")
            addManualBook()
        }

        binding.filterBookButton.setOnClickListener {
            println("Filter Book Button Clicked")
            showSelectSubjectDialog()
        }



        return root
    }

    private val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val book = bookAdapter.currentList[viewHolder.adapterPosition] // Get swiped book
            val position = viewHolder.adapterPosition

            AlertDialog.Builder(requireContext())
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete '${book.title}'?")
                .setPositiveButton("Yes") { _, _ ->
                    bookAdapter.notifyItemRemoved(position)
                    lifecycleScope.launch {
                        bookViewModel.removeBook(book) // Remove from database
                    }
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    bookAdapter.notifyItemChanged(viewHolder.adapterPosition) // Reset item so it's not removed visually
                }
                .show()
        }
    }

    private fun addManualBook() {
        //Create a new book with no data, and no subjects
        val newBook = BookEntry(
            title = "",
            author = "",
            lccn = "",
            location = "",
            isbn = "",
            url = ""
        )
        lifecycleScope.launch {
            val bookId = bookViewModel.addBook(newBook).toInt() // Add to database
            val subjects: List<Subject> = emptyList() // No subjects for new book
            subjects.forEach { subject ->
                bookViewModel.insertSubject(subject)
                linkSubjectsToBook(bookId, subjects)
            }

            val intent = Intent(requireContext(), BookDetails::class.java).apply {
                putExtra("bookId", bookId)
            }
            startActivity(intent) // Start BookDetails activity
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
            val listView = ListView(requireContext()).apply {
                adapter = ArrayAdapter(
                    requireContext(),
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

            AlertDialog.Builder(requireContext())
                .setTitle("Filter Books by Subject")
                .setView(listView) // Set the scrollable ListView as the dialog content
                .setPositiveButton("Filter") { _, _ ->
                    // Add selected subjects to the adapter
                    val newSubjects = subjectNames.filterIndexed { index, _ -> selectedSubjects[index] }
                    // Turn the subjects into a list of strings
                    val subjectsList = newSubjects.map { it.toString() }
                    println("Selected subjects: $subjectsList")
                    bookViewModel.filterBooks(subjectsList)

                    updateFilteredBooks()

                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateFilteredBooks() {
        bookViewModel.filteredBooks.observe(viewLifecycleOwner) { books ->
            println("Filtered books: ${books.size}")
            books.forEach { book ->
                println("Book: ${book.book.title}, Subjects: ${book.subjects.joinToString { it.subjectName }}")
            }
            val bookEntries = books.map { it.book }
            bookAdapter.submitList(bookEntries) // Update the adapter with filtered books
            bookAdapter.notifyDataSetChanged()
        }
    }

    suspend fun linkSubjectsToBook(bookId: Int, subjects: List<Subject>) {
        val refs = subjects.map { subject ->
            BookSubjectCrossRef(bookId = bookId, subjectName = subject.subjectName)
        }
        bookViewModel.insertCrossRefs(refs)  // Again, use `@Insert(onConflict = IGNORE)`
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}