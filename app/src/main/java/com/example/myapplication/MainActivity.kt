package com.example.myapplication

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

    private fun fetchWebpage(url: String, callback: (BookEntry?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //Get the webpage
                val document = Jsoup.connect(url).get()

                //Process the webpage data
                //Get the Title:
                val mainTitleElement: Element? = document.selectFirst("h3.item-title:contains(Main title)")
                val mainTitleText: String? = mainTitleElement?.let {
                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
                }

                //Get the Authors name:
                val authorNameElement: Element? = document.selectFirst("h3.item-title:contains(Personal name)")
                val authorNameText: String? = mainTitleElement?.let {
                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
                }

                //Get the Subjects name:
                val subjectsElement: Element? = document.selectFirst("h3.item-title:contains(LC Subjects)")
                val subjectsText: String? = mainTitleElement?.let {
                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
                }

                //Get the LCCN:
                val lccnElement: Element? = document.selectFirst("h3.item-title:contains(LCCN)")
                val lccnText: String? = mainTitleElement?.let {
                    it.nextElementSibling()?.selectFirst("span[dir=ltr]")?.text()
                }

                val bookEntry = BookEntry(
                    authorName = authorNameText,
                    mainTitle = mainTitleText,
                    subjects = subjectsText,
                    lccn = lccnText
                )

                //return from the function
                withContext(Dispatchers.Main) {
                    callback(bookEntry)
                }
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

        checkPermission(android.Manifest.permission.CAMERA, 100)

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