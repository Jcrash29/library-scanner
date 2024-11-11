package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
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
import org.jsoup.nodes.Element
import java.lang.Thread.sleep
import java.net.HttpURLConnection
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class BookEntry(
    val authorName: String?,
    val mainTitle: String?,
    val subjects: String?,
    val lccn: String?,
)


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
    }

    private fun getWebpageHtml(url: String): String {
        val url_2 = URL(url)
        val urlConnection = url_2.openConnection() as HttpURLConnection

        /* Set a property onto our web request. Simulate a browser? */
        //urlConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR 1.2.30703)");
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");

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

    private fun fetchWebpage(url: String, callback: (BookEntry?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //Get the data from the webpage
                var document = getWebpageHtml(url)

                // Check the Webpage data, and re-load the page if it failed initially.
                var retries = 5
                val titlePattern = "<title>(.*?)</title>".toRegex()
                while(retries > 0) {
                    val matchResult = titlePattern.find(document.toString())
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
                        document = getWebpageHtml(url)
                    }

                }
                //Process the webpage data
                //Get the Title:
//                val mainTitleElement: Element? = document.selectFirst("h3.item-title:contains(Main title)")
//                val mainTitleText: String? = mainTitleElement?.let {
//                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
//                }
//
//                //Get the Authors name:
//                val authorNameElement: Element? = document.selectFirst("h3.item-title:contains(Personal name)")
//                val authorNameText: String? = mainTitleElement?.let {
//                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
//                }
//
//                //Get the Subjects name:
//                val subjectsElement: Element? = document.selectFirst("h3.item-title:contains(LC Subjects)")
//                val subjectsText: String? = mainTitleElement?.let {
//                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
//                }
//
//                //Get the LCCN:
//                val lccnElement: Element? = document.selectFirst("h3.item-title:contains(LCCN)")
//                val lccnText: String? = mainTitleElement?.let {
//                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
//                }

//                val bookEntry = BookEntry(
//                    authorName = authorNameText,
//                    mainTitle = mainTitleText,
//                    subjects = subjectsText,
//                    lccn = lccnText
//                )

//                //return from the function
//                withContext(Dispatchers.Main) {
//                    callback(bookEntry)
//                }
            } catch (e: Exception) {
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

                val preURL  = "https://catalog.loc.gov/vwebv/search?searchCode=STNO&searchArg="
                val postURL = "&searchType=1&limitTo=none&fromYear=&toYear=&limitTo=LOCA%3Dall&limitTo=PLAC%3Dall&limitTo=TYPE%3Dall&limitTo=LANG%3Dall&recCount=25"

                val fullURL = preURL.plus(barcode.rawValue).plus(postURL)

                println(fullURL)

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