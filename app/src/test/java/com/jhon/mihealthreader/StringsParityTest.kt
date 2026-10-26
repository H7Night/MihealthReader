package com.jhon.mihealthreader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class StringsParityTest {
    private fun keys(path: String): Set<String> {
        val file = listOf(File(path), File("app/$path")).firstOrNull { it.exists() }
            ?: error("missing $path")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { (nodes.item(it) as Element).getAttribute("name") }
            .toSet()
    }

    @Test
    fun `values and values-zh have identical keys`() {
        assertEquals(
            keys("src/main/res/values/strings.xml"),
            keys("src/main/res/values-zh/strings.xml"),
        )
    }
}
