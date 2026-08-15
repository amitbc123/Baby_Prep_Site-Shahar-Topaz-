package com.oryareach.core.domain.shopping

import com.oryareach.core.model.Priority
import com.oryareach.core.model.ShoppingAlternative
import com.oryareach.core.model.ShoppingCategory
import com.oryareach.core.model.ShoppingItem
import com.oryareach.core.model.ShoppingStatus
import io.kotest.matchers.shouldBe
import org.junit.Test

private fun item(
    category: ShoppingCategory = ShoppingCategory.NURSERY,
    estimatedPrice: Int? = null,
    actualPrice: Int? = null,
    status: ShoppingStatus = ShoppingStatus.NEED,
    alternatives: List<ShoppingAlternative> = emptyList(),
    chosenAlternativeId: String? = null,
) = ShoppingItem(
    id = "id",
    name = "item",
    category = category,
    estimatedPrice = estimatedPrice,
    actualPrice = actualPrice,
    priority = Priority.NORMAL,
    status = status,
    alternatives = alternatives,
    chosenAlternativeId = chosenAlternativeId,
)

class BudgetTest {

    @Test
    fun `itemEffectivePrice prefers actualPrice over everything else`() {
        itemEffectivePrice(item(actualPrice = 100, estimatedPrice = 50)) shouldBe 100
    }

    @Test
    fun `itemEffectivePrice falls back to the chosen alternative price`() {
        val i = item(
            estimatedPrice = 50,
            chosenAlternativeId = "alt-1",
            alternatives = listOf(ShoppingAlternative(id = "alt-1", name = "alt", price = 80)),
        )
        itemEffectivePrice(i) shouldBe 80
    }

    @Test
    fun `itemEffectivePrice falls back to estimatedPrice when nothing else is set`() {
        itemEffectivePrice(item(estimatedPrice = 50)) shouldBe 50
    }

    @Test
    fun `itemEffectivePrice is null when no price is known`() {
        itemEffectivePrice(item()) shouldBe null
    }

    @Test
    fun `calculateBudget only counts spent for bought items`() {
        val totals = calculateBudget(
            listOf(
                item(status = ShoppingStatus.BOUGHT, actualPrice = 100),
                item(status = ShoppingStatus.NEED, estimatedPrice = 40),
            ),
        )
        totals.spentTotal shouldBe 100
        totals.estimatedTotal shouldBe 140
        totals.boughtCount shouldBe 1
        totals.totalCount shouldBe 2
    }

    @Test
    fun `calculateBudget groups totals by category`() {
        val totals = calculateBudget(
            listOf(
                item(category = ShoppingCategory.CLOTHING, estimatedPrice = 20),
                item(category = ShoppingCategory.CLOTHING, estimatedPrice = 30),
                item(category = ShoppingCategory.FEEDING, estimatedPrice = 10),
            ),
        )
        totals.byCategory.first { it.category == ShoppingCategory.CLOTHING }.estimated shouldBe 50
    }

    @Test
    fun `calculateBudget handles an empty list`() {
        val totals = calculateBudget(emptyList())
        totals.estimatedTotal shouldBe 0
        totals.spentTotal shouldBe 0
        totals.boughtCount shouldBe 0
        totals.totalCount shouldBe 0
        totals.byCategory shouldBe emptyList()
    }
}
