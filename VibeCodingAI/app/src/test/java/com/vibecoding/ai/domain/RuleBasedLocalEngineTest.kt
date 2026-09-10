package com.vibecoding.ai.domain

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedLocalEngineTest {
    @Test fun generatesAndroidProject() = runBlocking {
        val result = RuleBasedLocalEngine().generate("Crie um PDV offline", emptyMap())
        val paths = result.files.map { it.path }.toSet()
        assertTrue("app/build.gradle.kts" in paths)
        assertTrue("app/src/main/AndroidManifest.xml" in paths)
        assertTrue("app/src/main/java/generated/app/MainActivity.kt" in paths)
    }

    @Test fun preservesExistingProjectWhenEditing() = runBlocking {
        val existing = mapOf("README.md" to "# Demo", "custom.txt" to "keep")
        val result = RuleBasedLocalEngine().generate("Adicione modo escuro", existing)
        val files = result.files.associate { it.path to it.content }
        assertTrue(files["custom.txt"] == "keep")
        assertTrue(files["README.md"]!!.contains("modo escuro"))
    }
}
