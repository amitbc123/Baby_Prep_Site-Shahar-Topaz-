package com.oryareach.core.domain.importer

import com.oryareach.core.model.Assignee
import com.oryareach.core.model.Priority
import com.oryareach.core.model.ShoppingCategory
import com.oryareach.core.model.ShoppingStatus
import com.oryareach.core.model.TaskCategory
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.junit.Test

private const val SAMPLE_EXPORT = """
{
  "version": 1,
  "settings": { "dueDate": "2026-12-25", "babyName": "תמר" },
  "shoppingItems": [
    {
      "id": "s1", "name": "עגלה", "category": "תינוקייה",
      "estimatedPrice": 1200, "priority": "high", "status": "need",
      "assignee": "שחר", "alternatives": [
        { "id": "a1", "name": "חלופה", "price": 900 }
      ],
      "chosenAlternativeId": "a1"
    }
  ],
  "tasks": [
    { "id": "t1", "title": "לארוז תיק", "category": "תיק ליולדת", "priority": "normal", "assignee": "שניהם", "done": false }
  ],
  "importantDates": [
    { "id": "d1", "date": "2026-11-01", "title": "בדיקה" }
  ]
}
"""

class WebImportMapperTest {

    @Test
    fun `parses a valid version-1 export`() {
        parseWebSnapshot(SAMPLE_EXPORT) shouldBe parseWebSnapshot(SAMPLE_EXPORT)
        val snapshot = parseWebSnapshot(SAMPLE_EXPORT)
        checkNotNull(snapshot)
        snapshot.settings.dueDate shouldBe "2026-12-25"
    }

    @Test
    fun `rejects a non-1 version`() {
        parseWebSnapshot(SAMPLE_EXPORT.replace("\"version\": 1", "\"version\": 2")) shouldBe null
    }

    @Test
    fun `maps Hebrew category, priority and assignee literals to enums`() {
        val snapshot = checkNotNull(parseWebSnapshot(SAMPLE_EXPORT))
        var counter = 0
        val imported = snapshot.toImportedSnapshot { "id-${counter++}" }

        imported.settings.dueDate shouldBe LocalDate(2026, 12, 25)
        imported.settings.babyName shouldBe "תמר"

        val item = imported.shoppingItems.single()
        item.category shouldBe ShoppingCategory.NURSERY
        item.priority shouldBe Priority.HIGH
        item.status shouldBe ShoppingStatus.NEED
        item.assignee shouldBe Assignee.PARTNER_ONE
        item.chosenAlternativeId shouldBe item.alternatives.single().id

        val task = imported.tasks.single()
        task.category shouldBe TaskCategory.HOSPITAL_BAG
        task.assignee shouldBe Assignee.BOTH

        imported.importantDates.single().date shouldBe LocalDate(2026, 11, 1)
    }

    @Test
    fun `unknown category literal falls back to Other`() {
        val json = SAMPLE_EXPORT.replace("\"תינוקייה\"", "\"קטגוריה-לא-קיימת\"")
        val snapshot = checkNotNull(parseWebSnapshot(json))
        val imported = snapshot.toImportedSnapshot { "id" }
        imported.shoppingItems.single().category shouldBe ShoppingCategory.OTHER
    }
}
