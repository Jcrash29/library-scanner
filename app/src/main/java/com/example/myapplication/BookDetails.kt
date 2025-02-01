package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText

class BookDetails : AppCompatActivity() {
     private var lastestBookEntry : BookEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_book_details)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bookDetailActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backButton = findViewById<Button>(R.id.BackToMain)
        backButton.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }


        val bookEntry: BookEntry? = intent.getParcelableExtra("book")

        val titleTextBox : EditText = findViewById(R.id.titleTextBox);
        val authorTextBox : EditText = findViewById(R.id.authorTextBox);

        titleTextBox.setText(bookEntry?.mainTitle)
        authorTextBox.setText(bookEntry?.authorName)


    }

    fun setLatestBookEntry(bookEntry: BookEntry?) {
        setContentView(R.layout.activity_book_details)
        lastestBookEntry = bookEntry


    }
}