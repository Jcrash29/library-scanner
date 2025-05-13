package entities

import androidx.room.Entity

@Entity(primaryKeys = ["bookId", "subjectName"])
data class BookSubjectCrossRef(
    val bookId: Int,
    val subjectName: String
)