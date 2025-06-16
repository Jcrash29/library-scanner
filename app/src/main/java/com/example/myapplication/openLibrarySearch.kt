package com.example.myapplication

import entities.BookEntry
import entities.Subject
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

            val bookEntry = BookEntry(
                title = getTitle(jsonResponse),
                author = getAuthorName(jsonResponse),
                lccn = "unknown",
                location = "Unknown",
                isbn = barCode,
                url = websiteURL
            )

            Result.success(
                Pair(
                    bookEntry,
                    getSubjects(jsonResponse)
                )
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

    // Parse the JSON response to get the author Name
    private fun getAuthorName(jsonResponse: String): String {
        return if(jsonResponse.contains("\"author_name\": [")) {
            jsonResponse.substringAfter("\"author_name\": [").substringAfter("\"").substringBefore("\"")

            //Standardize the string by setting ONLY the first letters to capital
                .split(" ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
        } else {
            "" // Return an empty string if no author name is found
        }
    }

    // Parse the JSON response to get the title
    private fun getTitle(jsonResponse: String): String {
        return if(jsonResponse.contains("\"title\": \"")) {
            jsonResponse.substringAfter("\"title\": \"").substringBefore("\"")
                //Standardize the string by setting ONLY the first letters to capital
                .split(" ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
        } else {
            "" // Return an empty string if no title is found
        }
    }

    // Parse the JSON response to get the subjects
    private fun getSubjects(jsonResponse: String): List<Subject> {
        if (jsonResponse.contains("\"subject\"")) {
            val subjects = jsonResponse.substringAfter("\"subject\": [").substringBefore("]")
            return subjects.split(",").map { Subject(it.trim().replace("\"","")) }
        } else {
            return emptyList() // Return an empty list if no subjects are found
        }
    }
}