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

    private val base = "src/commonMain/composeResources"
    private val itemRegex = Regex("""<item[ >]""")
    private val arrayRegex = Regex("""<string-array\s+name="([^"]+)"[^>]*>(.*?)</string-array>""", RegexOption.DOT_MATCHES_ALL)
    private val stringRegex = Regex("""<string\s+name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholderRegex = Regex("""%\d+\$[a-zA-Z]""")

    @Test
    fun `string arrays have the same item count in both catalogs`() {
        fun counts(path: String) =
            arrayRegex.findAll(File(path).readText()).associate { it.groupValues[1] to itemRegex.findAll(it.groupValues[2]).count() }
        val en = counts("$base/values/strings.xml")
        val es = counts("$base/values-es/strings.xml")
        assertTrue(en.isNotEmpty(), "No string-arrays found")
        assertTrue(en == es, "Array item counts differ: en=$en es=$es")
    }

    @Test
    fun `strings use the same placeholders in both catalogs`() {
        fun placeholders(path: String) =
            stringRegex.findAll(File(path).readText()).associate { it.groupValues[1] to placeholderRegex.findAll(it.groupValues[2]).map { m -> m.value }.toSet() }
        val en = placeholders("$base/values/strings.xml")
        val es = placeholders("$base/values-es/strings.xml")
        val diffs = en.filter { (k, v) -> es[k] != null && es[k] != v }.keys
        assertTrue(en.values.any { it.isNotEmpty() }, "No placeholders found; regex broken")
        assertTrue(diffs.isEmpty(), "Placeholder mismatch for: $diffs")
    }
}
