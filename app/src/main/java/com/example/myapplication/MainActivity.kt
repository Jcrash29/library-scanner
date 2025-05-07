


package com.example.myapplication

import com.example.myapplication.ui.main.BookAdapter
import com.example.myapplication.data.repository.BookRepository
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.model.BookEntry
import com.example.myapplication.ui.ViewBooksActivity
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
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
            val intent = Intent(this,ViewBooksActivity::class.java)
            startActivity(intent)
        }


    }

    private fun getWebpageHtml(url: String): Result<String> {
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
        urlConnection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
        urlConnection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
        //urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate")
        urlConnection.setRequestProperty("Connection", "keep-alive")

        // Add screen-related headers
        urlConnection.setRequestProperty("Viewport-Width", "1920")
        urlConnection.setRequestProperty("Viewport-Height", "1080")
        urlConnection.setRequestProperty("DPR", "1")  // Device Pixel Ratio
        urlConnection.setRequestProperty("Width", "1920")
        urlConnection.setRequestProperty("Height", "1080")

        /* Perform a test connection. Not needed? */
        urlConnection.requestMethod = "GET"
        try {
            urlConnection.connect()
        } catch(e: IOException) {
            return Result.failure(e)
        }
        val responseCode = urlConnection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            println("Connection good")
        } else {
            println("Connection Failed")
        }

        try {
            val test = urlConnection.inputStream.bufferedReader().readText()
            println("We got something ")
            return Result.success(test)
        } finally {
            urlConnection.disconnect()
        }
    }

    fun extractAuthor(element: Element): String {

        // Find the "Personal name" section
        var author = element.select("name[type=personal][usage=primary] > namePart").text()

        // Remove the ", author." suffix if present
        if(author.last() == ',')
        {
            author.dropLast(1)
        }
        return author
    }

    fun extractTitle(element: Element): String {
        var title = element.select("titleInfo > title").text()

        /* Capitalize each word of the title */
        title = title.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercaseChar() } }

        return title
    }

    fun extractSubjects(element: Element): List<String> {
        // Select the "LC Subjects" section
        val topics = element.select("subject > topic").eachText()  // Using Set to avoid duplicates

        // Extract all subjects from the <span> tags inside <li> elements within this section
        return topics
    }

    fun extractLCCN(element: Element): String {
        // Find the "Personal name" section
        val lccn = element.select("identifier[type=lccn]").text()
        return lccn
    }

//    private fun fetchWebpage(url: String, callback: (BookEntry) -> Unit) : BookEntry {
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                //Get the data from the webpage
//                var html = getWebpageHtml(url)
//
//                /* convert the HTML into a JSOUP Documents type */
//                val document: Document = Jsoup.parse(html, "", org.jsoup.parser.Parser.xmlParser())
//
//                val numberOfRecordsText = document.select("zs\\:numberOfRecords").text()
//                if (numberOfRecordsText == "0") {
//                    throw IllegalStateException("Unable to find record in XML")
//                }
//
//                val records = document.getElementsByTag("zs:record")
//                val firstRecord = records.first() ?: Element("N/a")
//
//                val bookEntry = BookEntry(
//                    author = extractAuthor(firstRecord),
//                    title = extractTitle(firstRecord),
//                    subjects = extractSubjects(firstRecord),
//                    lccn = extractLCCN(firstRecord),
//                    location = "Not Set"
//                )
//
//                //return from the function
//                withContext(Dispatchers.Main) {
//                    callback(bookEntry)
//                }
//            } catch (e: Exception) {
//                println("Error: ${e.message}")
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    callback(
//                        BookEntry(
//                            author = "n/a",
//                            title = "n/a",
//                            subjects = listOf("n/a"),
//                            lccn = "n/a",
//                            location = "n/a"
//                        )
//                    )
//                }
//            }
//        }
//    }

    private fun fetchWebpage(url: String) : Result<BookEntry> {
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
                    return Result.failure(IllegalStateException("Unable to find record in XML"))
                }

                val records = document.getElementsByTag("zs:record")
                val firstRecord = records.first() ?: Element("N/a")

                val bookEntry = BookEntry(
                    author = extractAuthor(firstRecord),
                    title = extractTitle(firstRecord),
                    subjects = extractSubjects(firstRecord),
                    lccn = extractLCCN(firstRecord),
                    location = "Not Set"
                )

                return Result.success(bookEntry)
            }
        return Result.failure(Exception("Not sure how we got here."))
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
    suspend fun scan(scanner : GmsBarcodeScanner) : Result<BookEntry> {
        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    println("Task completed successfully")
                    println(barcode.rawValue)

                    val preURL =
                        "http://lx2.loc.gov:210/lcdb?version=1.1&operation=searchRetrieve&query=%22"
                    val postURL = "%22&startRecord=1&maximumRecords=5&recordSchema=mods"
                    val fullURL = preURL.plus(barcode.rawValue).plus(postURL)
                    println(fullURL)

                    // Launch a coroutine on IO dispatcher for the network call
                    // (Alternatively, if fetchWebpage is a suspend function, you could call it directly)
                    kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                        val result = fetchWebpage(fullURL) // Assume this returns Result<BookEntry>
                        cont.resume(result)  // Resume the continuation with the network result
                    }
                }
                .addOnCanceledListener {
                    cont.resume(Result.failure<BookEntry>(Exception("Scan canceled")))
                }
                .addOnFailureListener { e ->
                    println("Task failed with an exception")
                    println(e.message)
                    cont.resume(Result.failure<BookEntry>(e))
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
                        val intent = Intent(this@MainActivity, BookDetails::class.java).apply {
                            putExtra("book", it)
                        }
                        startActivity(intent)
                    }
                    .onFailure { e ->
                        errorFindingBookMsg()
                        println("Failed: ${e.message}")
                    }
            } catch(e: Exception) {
                println("Exception during scan: ${e.message}")
            }
        }
    }
}