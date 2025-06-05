package com.example.myapplication.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.BookDetails
import com.example.myapplication.LibraryOfCongressSearch
import com.example.myapplication.OpenLibrarySearch
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class DashboardFragment : Fragment() {

    private lateinit var bookAdapter: BookAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(requireContext()).bookDao()))
    }

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.scanButton.setOnClickListener { view ->
            scanClick(view)
        }

        scanClick(view)

//        val textView: TextView = binding.textDashboard
//        dashboardViewModel.text.observe(viewLifecycleOwner) {
//            textView.text = it
//        }
        return root
    }

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(requireContext() as Activity, arrayOf(permission), requestCode)
        } else {
            println("Permission already granted")
        }
    }

    fun scanClick(view: View?){
        println("Scan Clicked!")

        checkPermission(Manifest.permission.CAMERA, 100)

        val scanner = GmsBarcodeScanning.getClient(requireContext())
        lifecycleScope.launch {
            try {
                val bookEntryResult = scan(scanner)
                bookEntryResult
                    .onSuccess {
                        val bookId = addBookProcess(it.first, it.second)

                        val intent = Intent(requireContext(), BookDetails::class.java).apply {
                            putExtra("bookId", bookId)
                        }
                        startActivity(intent)
                    }
                    .onFailure { e ->
                        errorFindingBookMsg()
                        println("Failed: ${e.message}")
                    }
            } catch(e: Exception) {
                println("Exception during scan: ${e.message}")
                AlertDialog.Builder(requireContext())
                    .setTitle("Unable to access Website")
                    .setMessage("Website might be down, or you are not connected to the internet. Website: http://lx2.loc.gov:210/lcdb")
                    .show()
            }
        }
    }

    private suspend fun addBookProcess(bookEntry: BookEntry, subjects: List<Subject>): Int {
        println("Entered add book process")
        val isDuplicate = bookViewModel.isDuplicate(bookEntry)
        println("Is duplicate: $isDuplicate")

        return if (isDuplicate) {
            val userConfirmed = showDuplicateDialog(bookEntry)
            if (userConfirmed) {
                withContext(Dispatchers.IO) {
                    bookViewModel.addBook(bookEntry)
                }.toInt().also { println("Book added with ID: $it") }
            } else {
                println("User canceled adding the duplicate book.")
                -1
            }
        } else {
            withContext(Dispatchers.IO) {
                bookViewModel.addBook(bookEntry)
            }.toInt().also { println("Book added with ID: $it") }
        }.also {
            if (it != -1) {
                insertSubjects(subjects)
                linkSubjectsToBook(it, subjects)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    suspend fun scan(scanner : GmsBarcodeScanner) : Result<Pair<BookEntry, List<Subject>>> {
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barCode ->
                    GlobalScope.launch(Dispatchers.IO)  {
                        LibraryOfCongressSearch().getBook(barCode.rawValue.toString())
                            .onSuccess { bookEntry ->
                                cont.resume(Result.success(bookEntry))
                            }
                            .onFailure {
                                OpenLibrarySearch()
                                    .getBook(barCode.rawValue.toString())
                                    .onSuccess { bookEntry ->
                                        cont.resume(Result.success(bookEntry))
                                    }
                                    .onFailure { e ->
                                        cont.resume(Result.failure<Pair<BookEntry, List<Subject>>>(e))
                                    }
                            }
                    }
                }
                .addOnCanceledListener {
                    cont.resume(Result.failure<Pair<BookEntry, List<Subject>>>(Exception("Scan canceled")))
                }
                .addOnFailureListener { e ->
                    println("Task failed with an exception")
                    println(e.message)
                    cont.resume(Result.failure<Pair<BookEntry, List<Subject>>>(e))
                }
        }
    }

    private suspend fun showDuplicateDialog(newBook: BookEntry): Boolean {
        return suspendCancellableCoroutine { cont ->
            AlertDialog.Builder(requireContext())
                .setTitle("Duplicate Book")
                .setMessage("A book with the title \"${newBook.title}\" already exists. Do you want to add it anyway?")
                .setPositiveButton("Yes") { _, _ ->
                    cont.resume(true)
                }
                .setNegativeButton("No") { dialog, _ ->
                    cont.resume(false)
                }
                .show()
        }
    }

    suspend fun insertSubjects(subjects: List<Subject>) {
        subjects.forEach { subject ->
            try {
                bookViewModel.insertSubject(subject)
            } catch (e: Exception) {
                Log.e("DB", "Failed to insert subject: ${subject.subjectName}", e)
            }
        }
    }

    suspend fun linkSubjectsToBook(bookId: Int, subjects: List<Subject>) {
        val refs = subjects.map { subject ->
            BookSubjectCrossRef(bookId = bookId, subjectName = subject.subjectName)
        }
        bookViewModel.insertCrossRefs(refs)  // Again, use `@Insert(onConflict = IGNORE)`
    }

    private fun errorFindingBookMsg() {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Unable to find this book")
            .setMessage("Consider retrying or manually entering")
            .setPositiveButton("Close", null)
            .create()

        dialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}