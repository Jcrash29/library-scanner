package com.example.myapplication.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.R
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class SetApi : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_api)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun apiSubmitClick(view: View?) {
        println("Submit the API")

        //https://stackoverflow.com/questions/45193941/how-to-read-and-write-txt-files-in-android-in-kotlin


        val location = File(view?.context?.filesDir, "/LibraryApp")
        //val location = File(Environment.getExternalStorageDirectory()+"/LibraryApp")
        //val String loc = Environment.getDataDirectory() + "/PhotoEditors";

        var success = true
        if (!location.exists())
            success = location.mkdir()

        if (success) {
            // directory exists or already created
            val file = File(location, "APIKey.txt")
            try {
                val textInputEditText: TextInputEditText  = findViewById(R.id.apiKeyInput);

                // response is the data written to file
                file.writeText(textInputEditText.getText().toString())
            } catch (e: Exception) {
                // handle the exception
                println("Unable to write the file")
            }

        } else {
            // directory creation is not successful
            println("Unable to create the document")
        }

    }
}