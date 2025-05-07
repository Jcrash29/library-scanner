package com.example.myapplication.data.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        // You can choose a delimiter that you know won't appear in your subjects.
        return value.joinToString(separator = ",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        // Handle empty strings gracefully.
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
}