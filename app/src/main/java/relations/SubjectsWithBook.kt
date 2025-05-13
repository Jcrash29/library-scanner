package relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject

data class SubjectWithBooks(
    @Embedded var subject: Subject,
    @Relation(
        parentColumn = "subjectName",
        entityColumn = "bookId",
        associateBy = Junction(BookSubjectCrossRef::class)
    )
    var books: List<BookEntry>
)