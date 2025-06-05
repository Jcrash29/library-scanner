package com.example.myapplication

import entities.BookEntry
import entities.Subject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class LibraryOfCongressSearch {

    suspend fun getBook(barCode : String) : Result<Pair<BookEntry, List<Subject>>>
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

    private fun extractAuthor(element: Element): String {

        // Find the "Personal name" section
        var author = element.select("name[type=personal][usage=primary] > namePart").text()

        // Remove the ", author." suffix if present
        if(author.lastOrNull() == ',')
        {
            println("Author before: $author")
            author = author.dropLast(1)
            println("Author  after: $author")
        }

        //Standardize the string by setting ONLY the first letters to capital
        author.split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }

        return author
    }

    private fun extractTitle(element: Element): String {
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

        //Standardize the string by setting ONLY the first letters to capital
        title.split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }

        return title
    }

    private fun extractSubjects(element: Element): List<Subject> {
        val topics = element.select("subject > topic").eachText()
        return topics.distinct().map { Subject(subjectName = it) }
    }

    private fun extractLCCN(element: Element): String {
        // Find the "Personal name" section
        val lccn = element.select("identifier[type=lccn]").text()
        return lccn
    }

}