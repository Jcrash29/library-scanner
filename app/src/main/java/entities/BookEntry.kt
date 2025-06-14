package entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "books")
data class BookEntry(
    @PrimaryKey(autoGenerate = true) val bookId: Int = 0,
    var title: String,
    val author: String,
    val lccn: String,
    val location: String,
    val isbn: String,
    val url: String
) : Parcelable