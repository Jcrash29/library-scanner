


package com.example.myapplication

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
import com.example.myapplication.ui.SetApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import java.net.URL
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BookEntry(
    val authorName: String?,
    val mainTitle: String?,
    val subjects: Set<String>?,
    val lccn: String?,
) : Parcelable


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    val bookDetails = BookDetails()

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


        val bookDetailButton = findViewById<Button>(R.id.BookDetails)
        bookDetailButton.setOnClickListener {
            val intent = Intent(this,BookDetails::class.java)
            startActivity(intent)
        }

        val homeSetAPIButton = findViewById<Button>(R.id.homeSetAPI)
        homeSetAPIButton.setOnClickListener {
            val intent = Intent(this,SetApi::class.java)
            startActivity(intent)
        }

    }

    private fun getWebpageHtml(url: String): String {
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
        urlConnection.connect()
        val responseCode = urlConnection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            println("Connection good")
        } else {
            println("Connection Failed")
        }

        try {
            val test = urlConnection.inputStream.bufferedReader().readText()
            println("We got something ")
            return test
        } finally {
            urlConnection.disconnect()
        }
    }

    fun extractAuthor(element: Element?): String? {

        // Find the "Personal name" section
        val author = element?.select("name[type=personal][usage=primary] > namePart")?.text()

        // Remove the ", author." suffix if present
        return author
    }

    fun extractTitle(element: Element?): String? {
        val title = element?.select("titleInfo > title")?.text()
        return title
    }

    fun extractSubjects(element: Element?): Set<String>? {
        // Select the "LC Subjects" section
        val topics = element?.select("subject > topic")?.map { it.text() }?.toSet()  // Using Set to avoid duplicates

        // Extract all subjects from the <span> tags inside <li> elements within this section
        return topics
    }

    fun extractLCCN(element: Element?): String? {
        // Find the "Personal name" section
        val lccn = element?.select("identifier[type=lccn]")?.text()
        return lccn
    }

    private fun fetchWebpage(url: String, callback: (BookEntry?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //Get the data from the webpage
                var html = getWebpageHtml(url)

                // Check the Webpage data, and re-load the page if it failed initially.
                var retries = 5
                val titlePattern = "<title>(.*?)</title>".toRegex()
                while(retries > 0) {
                    val matchResult = titlePattern.find(html.toString())
                    val title = matchResult?.groups?.get(1)?.value

                    val isTitleNoConnection = title == "LC Catalog - No Connections Available"

                    if(!isTitleNoConnection)
                    {
                        retries = 0
                    }
                    else
                    {
                        retries -= 1
                        sleep(3000)
                        //document = Jsoup.connect(url).get()
                        html = getWebpageHtml(url)
                    }
                }

                /* convert the HTML into a JSOUP Documents type */
                val document: Document = Jsoup.parse(html, "", org.jsoup.parser.Parser.xmlParser())

                val records = document.getElementsByTag("zs:record")
                val firstRecord = records.first()


                val bookEntry = BookEntry(
                    authorName = extractAuthor(firstRecord),
                    mainTitle = extractTitle(firstRecord),
                    subjects = extractSubjects(firstRecord),
                    lccn = extractLCCN(firstRecord)
                )

                //return from the function
                withContext(Dispatchers.Main) {
                    callback(bookEntry)
                }
            } catch (e: Exception) {
                println("Error: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }



    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            println("Permission already granted")
        }
    }


    fun scanClick(view: View?){
        println("Scan Clicked!")

        checkPermission(Manifest.permission.CAMERA, 100)

        val scanner = GmsBarcodeScanning.getClient(this)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                println("Task completed successfully")
                println(barcode.rawValue)

                val preURL  = "http://lx2.loc.gov:210/lcdb?version=1.1&operation=searchRetrieve&query=%22"
                val postURL = "%22&startRecord=1&maximumRecords=5&recordSchema=mods"

                val fullURL = preURL.plus(barcode.rawValue).plus(postURL)

                println(fullURL)

                //val bookDetails = BookDetails()

                fetchWebpage(fullURL) { bookEntry: BookEntry? ->
                    bookEntry?.let {
                        // Handle the result here, e.g., update the UI
                        println("MainActivity Main Title Text: ${it.mainTitle}")
                        println("MainActivity The Authors Name: ${it.authorName}")
                        println("MainActivity The Subject: ${it.subjects}")
                        println("MainActivity The LCCN: ${it.lccn}")
                    } ?: run {
                        println("MainActivity Failed to fetch the webpage")
                    }

                    val intent = Intent(this,BookDetails::class.java).apply {
                        putExtra("book", bookEntry)
                    }
                    startActivity(intent)

                }



            }
            .addOnCanceledListener {
                println("Task canceled")
            }
            .addOnFailureListener { e ->
                println("Task failed with an exception")
                println(e.message)
            }
    }
}