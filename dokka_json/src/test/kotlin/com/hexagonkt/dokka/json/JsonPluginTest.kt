package com.hexagonkt.dokka.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.hexagonkt.core.requirePath
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import kotlin.test.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS.LINUX
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JsonPluginTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        moduleName = "mod"

        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    @EnabledOnOs(LINUX) // TODO Fix this test for other OSs
    fun `JsonPlugin serialize documentation to a JSON file`() {
        testInline(
            """
            |/src/main/kotlin/sample/Test.kt
            |package sample
            |/**
            | * Data class description. Detailed information.
            | *
            | * @property reason Class property.
            | */
            |data class TestingIsEasy(val reason: String) {
            |  /**
            |   * Method definition.
            |   *
            |   * @param parameter Method parameter.
            |   * @return The return description.
            |   */
            |  fun docFun(parameter: String): Int = 1
            |}
            |/**
            | *  # Header
            | *  * List item
            | *
            | *  ## Numbers
            | *  1. Numbered item
            | *
            | *  ### Paragraphs
            | *  `file.json` contains settings.
            | *
            | *  #### Nested items
            | *  5. Section
            | *      * Subsection
            | */
            |data class Class2(val property: Int)
            """.trimIndent(), configuration
        ) {
            documentablesTransformationStage = { module ->
                val sample = module.packages.find { it.name == "sample" }
                assertNotNull(sample)
                assertNotNull(sample.classlikes.find { it.name == "TestingIsEasy" })
            }
        }

        val mapper = ObjectMapper()
        val json = mapper.readValue(File("build/dokka").resolve("module_mod.json"), Map::class.java)
        assertEquals("mod", json.requirePath("name"))
    }
}
