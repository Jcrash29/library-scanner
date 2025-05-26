


package com.example.myapplication

import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.data.repository.BookRepository
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import com.example.myapplication.data.database.BookDatabase
import entities.BookEntry
import com.example.myapplication.ui.ViewBooksActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import entities.BookSubjectCrossRef
import entities.Subject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bookAdapter: BookAdapter


    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(this).bookDao()))
    }

    private fun errorFindingBookMsg() {
        val dialog = AlertDialog.Builder(this@MainActivity)
            .setTitle("Unable to find this book")
            .setMessage("Consider retrying or manually entering")
            .setPositiveButton("Close", null)
            .create()

        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val homeSetAPIButton = findViewById<Button>(R.id.homeViewBooks)
        homeSetAPIButton.setOnClickListener {
            val intent = Intent(this, ViewBooksActivity::class.java)
            startActivity(intent)
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

    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            println("Permission already granted")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    suspend fun scan(scanner : GmsBarcodeScanner) : Result<Pair<BookEntry, List<Subject>>> {
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barCode ->
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO)  {
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
            AlertDialog.Builder(this@MainActivity)
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

    fun scanClick(view: View?){
        println("Scan Clicked!")

        checkPermission(Manifest.permission.CAMERA, 100)

        val scanner = GmsBarcodeScanning.getClient(this)
        lifecycleScope.launch {
            try {
                val bookEntryResult = scan(scanner)
                bookEntryResult
                    .onSuccess {
                        val bookId = addBookProcess(it.first, it.second)

                        val intent = Intent(this@MainActivity, BookDetails::class.java).apply {
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
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Unable to access Website")
                    .setMessage("Website might be down, or you are not connected to the internet. Website: http://lx2.loc.gov:210/lcdb")
                    .show()
            }
        }
    }
}