


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

    private suspend fun getWebpageHtml(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            val url_2 = URL(url)
            val urlConnection = url_2.openConnection() as HttpURLConnection

            /* Set a property onto our web request. Simulate a browser? */
            val userAgents = listOf(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.63 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100317 Firefox/3.6.2"
            )
            urlConnection.setRequestProperty("User-Agent", userAgents.random())

            /* Set other properties to avoid bot detection */
            urlConnection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
            )
            urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
            //urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate")
            urlConnection.setRequestProperty("Connection", "keep-alive")

            // Add screen-related headers
            urlConnection.setRequestProperty("Viewport-Width", "1920")
            urlConnection.setRequestProperty("Viewport-Height", "1080")
            urlConnection.setRequestProperty("DPR", "1")  // Device Pixel Ratio
            urlConnection.setRequestProperty("Width", "1920")
            urlConnection.setRequestProperty("Height", "1080")

            try {
                urlConnection.connect()
                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("Connection good")
                    val test = urlConnection.inputStream.bufferedReader().readText()
                    Result.success(test)
                } else {
                    println("Connection Failed")
                    Result.failure(IOException("HTTP error code: $responseCode"))
                }
            } catch (e: IOException) {
                Result.failure(e)
            } finally {
                urlConnection.disconnect()
            }
        }
    }

    fun extractAuthor(element: Element): String {

        // Find the "Personal name" section
        var author = element.select("name[type=personal][usage=primary] > namePart").text()

        // Remove the ", author." suffix if present
        if(author.last() == ',')
        {
            println("Author before: $author")
            author = author.dropLast(1)
            println("Author  after: $author")
        }

        return author
    }

    fun extractTitle(element: Element): String {
        val titleElement = element.selectFirst("titleInfo > title")
        var title = titleElement?.text()


        /* Capitalize each word of the title */
        if(title != null) {
            title = title.split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }
        }
        else
        {
            println("Title not found")
            title = "N/A"
        }
        return title
    }

    fun extractSubjects(element: Element): List<Subject> {
        val topics = element.select("subject > topic").eachText()
        return topics.distinct().map { Subject(subjectName = it) }
    }

    fun extractLCCN(element: Element): String {
        // Find the "Personal name" section
        val lccn = element.select("identifier[type=lccn]").text()
        return lccn
    }

    private suspend fun fetchWebpage(url: String) : Result<Pair<BookEntry, List<Subject>>> {
        //Get the data from the webpage
        val html = getWebpageHtml(url)
        html
            .onFailure { e ->
                return Result.failure(e)
            }
            .onSuccess { webHTML ->
                /* convert the HTML into a JSOUP Documents type */
                val document: Document = Jsoup.parse(webHTML, "", org.jsoup.parser.Parser.xmlParser())

                val numberOfRecordsText = document.getElementsByTag("zs:numberOfRecords").text()
                if (numberOfRecordsText == "0") {
                    return Result.failure<Pair<BookEntry, List<Subject>>>(IllegalStateException("Unable to find record in XML"))
                }

                val records = document.getElementsByTag("zs:record")
                val firstRecord = records.first() ?: Element("N/a")

                val bookEntry = BookEntry(
                    author = extractAuthor(firstRecord),
                    title = extractTitle(firstRecord),
                    lccn = extractLCCN(firstRecord),
                    location = "Not Set",
                    isbn = "Not Set", //TODO: set this
                    url = url
                )

                val subjects = extractSubjects(firstRecord)

                println("Decoded book: ${bookEntry.title}")
                println("Book Subjects: ${subjects.map { it.subjectName }}")

                return Result.success(Pair(bookEntry, subjects))
            }
        return Result.failure<Pair<BookEntry, List<Subject>>>(Exception("Not sure how we got here."))

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

    private suspend fun checkLibraryOfCongress(barCode : String) : Result<Pair<BookEntry, List<Subject>>>
    {
        println("Task completed successfully")
        println(barCode)

        val preURL =
            "http://lx2.loc.gov:210/lcdb?version=1.1&operation=searchRetrieve&query=%22"
        val postURL = "%22&startRecord=1&maximumRecords=1&recordSchema=mods"
        val fullURL = preURL.plus(barCode).plus(postURL)
        println(fullURL)

        return fetchWebpage(fullURL)
    }


    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    suspend fun scan(scanner : GmsBarcodeScanner) : Result<Pair<BookEntry, List<Subject>>> {
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barCode ->
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO)  {
                        checkLibraryOfCongress(barCode.rawValue.toString())
                            .onSuccess { bookEntry ->
                                cont.resume(Result.success(bookEntry))
                            }
                            .onFailure {
                                openLibrarySearch()
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