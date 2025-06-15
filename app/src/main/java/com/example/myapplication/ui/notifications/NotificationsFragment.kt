package com.example.myapplication.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.FragmentNotificationsBinding
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.launch
import java.io.*

class NotificationsFragment : Fragment() {

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(requireContext()).bookDao()))
    }

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.exportButton.setOnClickListener {
            exportToCSV()
        }

        binding.importButton.setOnClickListener {
            importFromCSV()
        }

        return root
    }

    private fun writeCSVFile(writer: BufferedWriter) {
        writer.write("")
        writer.newLine()
        bookViewModel.allBooks.observe(viewLifecycleOwner) { books ->
            for (book in books) {
                writer.write("${book.book.title},${book.book.author},${book.book.lccn},${book.book.isbn},${book.book.url},${book.book.location},")
                // Add all subjects to the line
                val subjects = book.subjects.joinToString(";") { it.subjectName }
                writer.write("[${subjects}]")
                writer.newLine()
            }
        }
    }

    private fun exportToCSV() {
        val fileName = "books.csv"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            val writer = BufferedWriter(FileWriter(file))
            writeCSVFile(writer)
            writer.close()

            // Prompt user for action
            val options = arrayOf("Save to Downloads", "Share File")
            val builder = android.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Export Options")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Toast.makeText(requireContext(), "File saved to Downloads: $fileName", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Books Data")
                            putExtra(Intent.EXTRA_TEXT, "Here is the exported books data.")
                        }
                        startActivity(Intent.createChooser(intent, "Share CSV"))
                    }
                }
            }
            builder.show()

        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromCSV() {
        val fileName = "books.csv"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found: $fileName", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val reader = BufferedReader(FileReader(file))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val data = line!!.split(",")
                if (data.size < 6) continue // Skip invalid lines

                val title = data[0]
                val author = data[1]
                val lccn = data[2]
                val isbn = data[3]
                val url = data[4]
                val location = data[5]
                // Parse subjects if available
                val subjectsString = if (data.size > 6) data[6].removeSurrounding("[", "]") else ""
                val subjectsList = subjectsString.split(";").map { it.trim() }.filter { it.isNotEmpty() }

                // Create a new book entry
                val newBook = BookEntry(
                    title = title,
                    author = author,
                    lccn = lccn,
                    location = location,
                    isbn = isbn,
                    url = url
                )
                lifecycleScope.launch {
                    val bookId = bookViewModel.addBook(newBook).toInt()
                    subjectsList.forEach { subjectName ->
                        val subject = Subject(subjectName)
                        bookViewModel.insertSubject(subject)
                    }
                    val refs = subjectsList.map { subject ->
                        BookSubjectCrossRef(bookId = bookId, subjectName = subject)
                    }
                    bookViewModel.insertCrossRefs(refs)
                }
            }
            reader.close()
            Toast.makeText(requireContext(), "Import successful", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Failed to import: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}