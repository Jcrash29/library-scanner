package relations

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import entities.BookEntry
import entities.BookSubjectCrossRef
import entities.Subject

data class BookWithSubjects(
    @Embedded var book: BookEntry,
    @Relation(
        parentColumn = "bookId",
        entityColumn = "subjectName",
        associateBy = Junction(BookSubjectCrossRef::class)
    )
    var subjects: List<Subject>
) {
    constructor() : this(BookEntry(author = "", lccn = "", location = "", title = "", isbn = "", url = ""), emptyList())
}