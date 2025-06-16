package com.example.myapplication.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            exportToTSV()
        }

        binding.importButton.setOnClickListener {
            importFromTSV()
        }

        return root
    }

    private fun writeTSVFile(file: File) {
        bookViewModel.allBooks.observe(viewLifecycleOwner) { books ->
            val writer = BufferedWriter(FileWriter(file))
            try {
                writer.write("")
                writer.newLine()
                for (book in books) {
                    writer.write("${book.book.title}\t${book.book.author}\t${book.book.lccn}\t${book.book.isbn}\t${book.book.url}\t${book.book.location}\t")
                    val subjects = book.subjects.joinToString(";") { it.subjectName }
                    writer.write("[${subjects}]")
                    writer.newLine()
                }
            } catch (e: IOException) {
                Toast.makeText(
                    requireContext(),
                    "Failed to write file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                writer.close()
            }
        }
    }

    private fun exportToTSV() {
        val fileName = "books.tsv"
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            writeTSVFile(file)

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
                            type = "text/tsv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Books Data")
                            putExtra(Intent.EXTRA_TEXT, "Here is the exported books data.")
                        }
                        startActivity(Intent.createChooser(intent, "Share TSV"))
                    }
                }
            }
            builder.show()

        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromTSV() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQUEST_CODE_IMPORT_CSV)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT_CSV && resultCode == AppCompatActivity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val data = line!!.split("\t")
                        if (data.size < 6) continue // Skip invalid lines

                        val title = data[0]
                        val author = data[1]
                        val lccn = data[2]
                        val isbn = data[3]
                        val url = data[4]
                        val location = data[5]
                        val subjectsString = if (data.size > 6) data[6].removeSurrounding("[", "]") else ""
                        val subjectsList = subjectsString.split(";").map { it.trim() }.filter { it.isNotEmpty() }

                        lifecycleScope.launch {

                            val newBook = BookEntry(
                                title = title,
                                author = author,
                                lccn = lccn,
                                location = location,
                                isbn = isbn,
                                url = url
                            )
                            val isDuplicate = bookViewModel.isDuplicate(newBook)
                            if(!isDuplicate) {
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
                    }
                    reader.close()
                    Toast.makeText(requireContext(), "Import successful", Toast.LENGTH_SHORT).show()
                } catch (e: IOException) {
                    Toast.makeText(requireContext(), "Failed to import: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMPORT_CSV = 1001
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}