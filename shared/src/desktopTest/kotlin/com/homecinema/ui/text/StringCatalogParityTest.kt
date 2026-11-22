package com.homecinema.ui.text

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class StringCatalogParityTest {
    private val nameRegex = Regex("""<(?:string|plurals|string-array)\s+name="([^"]+)"""")

    private fun keys(path: String): Set<String> =
        nameRegex.findAll(File(path).readText()).map { it.groupValues[1] }.toSet()

    @Test
    fun `english and spanish catalogs define the same keys`() {
        val base = "src/commonMain/composeResources"
        val en = keys("$base/values/strings.xml")
        val es = keys("$base/values-es/strings.xml")
        assertTrue(en.isNotEmpty(), "English catalog has no keys")
        val missingInEs = en - es
        val missingInEn = es - en
        assertTrue(
            missingInEs.isEmpty() && missingInEn.isEmpty(),
            "Missing in values-es: $missingInEs; missing in values: $missingInEn",
        )
    }
}
