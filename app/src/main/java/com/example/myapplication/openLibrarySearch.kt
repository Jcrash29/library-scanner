package com.example.myapplication

import entities.BookEntry
import entities.Subject
import org.json.JSONObject
import java.net.URL

class OpenLibrarySearch {

    // Get the bookEntry and subjects from the openLibrary website
    fun getBook(barCode: String): Result<Pair<BookEntry, List<Subject>>> {
        return try {
            val websiteURL = generateURL(barCode)
            println("OpenLibrarySearch URL: $websiteURL")
            val jsonResponse = URL(websiteURL).readText()

            if (wasNoBookFound(jsonResponse)) {
                return Result.failure(Exception("No book found"))
            }

            if (jsonResponse.isEmpty()) {
                println("Error: JSON response is empty or invalid.")
                return Result.failure(Exception("No book found"))
            }

//            val jsonResponseCleaned = extractDocs(jsonResponse)

            val bookEntry = BookEntry(
                title = getTitle(jsonResponse),
                author = getAuthorName(jsonResponse),
                lccn = "unknown",
                location = "Unknown",
                isbn = barCode,
                url = websiteURL
            )

            println("Decoded book from OpenLibrary: ${bookEntry.title} by ${bookEntry.author}")
            if (bookEntry.title.isEmpty() || bookEntry.author.isEmpty()) {
              return Result.failure(Exception("No book found"))
            }

            val subjects = getSubjects(jsonResponse)
            println("Found ${subjects.size} subjects")
            val outputBook = Pair( bookEntry, subjects)
            Result.success(
                outputBook
            )
        } catch (e: Exception) {
            Result.failure(e) // Return the exception as a failure result
        }
    }

    private fun generateURL(barCode: String): String {
        return "https://openlibrary.org/search.json?isbn=$barCode&fields=subject,title,author_name"
    }

    private fun wasNoBookFound(jsonResponse: String): Boolean {
        return jsonResponse.contains("\"numFound\": 0")
    }

    /* Sometimes the authors names comes with brackets and quotes, so we need to clean that up */
    private fun cleanAuthorName(rawAuthor: String): String {
        return rawAuthor.replace("[", "").replace("]", "").replace("\"", "").replace("'", "")
    }

    // Parse the JSON response to get the author Name
    private fun getAuthorName(jsonResponse: String): String {
        println("OpenLibrarySearch: Parsing title from JSON response")
        return try {
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("docs")) {
                val docsArray = jsonObject.getJSONArray("docs")
                if (docsArray.length() > 0) {
                    val firstDoc = docsArray.getJSONObject(0)
                    if (firstDoc.has("author_name")) {
                        val author = cleanAuthorName(firstDoc.getString("author_name"))
                        author
                    } else {
                        println("No author found in the first document")
                        ""
                    }
                } else {
                    println("Docs array is empty")
                    ""
                }
            } else {
                println("No docs array found in JSON response")
                ""
            }
        } catch (e: Exception) {
            println("Error parsing title: ${e.message}")
            ""
        }
    }

    // Parse the JSON response to get the title
    private fun getTitle(jsonResponse: String): String {
        println("OpenLibrarySearch: Parsing title from JSON response")
        return try {
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("docs")) {
                val docsArray = jsonObject.getJSONArray("docs")
                if (docsArray.length() > 0) {
                    val firstDoc = docsArray.getJSONObject(0)
                    if (firstDoc.has("title")) {
                        val title = firstDoc.getString("title")
                        title
                    } else {
                        println("No title found in the first document")
                        ""
                    }
                } else {
                    println("Docs array is empty")
                    ""
                }
            } else {
                println("No docs array found in JSON response")
                ""
            }
        } catch (e: Exception) {
            println("Error parsing title: ${e.message}")
            ""
        }
    }

    // Parse the JSON response to get the subjects
    private fun getSubjects(jsonResponse: String): List<Subject> {
        println("OpenLibrarySearch: Parsing subjects from JSON response")
        return try {
            val jsonObject = JSONObject(jsonResponse)
            if (jsonObject.has("docs")) {
                val docsArray = jsonObject.getJSONArray("docs")
                if (docsArray.length() > 0) {
                    val firstDoc = docsArray.getJSONObject(0)
                    if (firstDoc.has("subject")) {
                        println("Found subject field in the first document")
                        val subjectsArray = firstDoc.getJSONArray("subject")
                        println("Subjects array length: ${subjectsArray.length()}")
                        (0 until subjectsArray.length())
                            .map { subjectsArray.getString(it).trim() }
                            .filter { it.isNotEmpty() }
                            .map { Subject(it) }
                    } else {
                        println("No subject field found in the first document")
                        emptyList()
                    }
                } else {
                    println("Docs array is empty")
                    emptyList()
                }
            } else {
                println("No docs array found in JSON response")
                emptyList()
            }
        } catch (e: Exception) {
            println("Error parsing subjects: ${e.message}")
            emptyList()
        }
    }
}