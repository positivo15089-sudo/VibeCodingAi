package com.vibecoding.ai.domain

class RuleBasedLocalEngine : AiEngine {
    override val displayName = "Motor local leve"

    override suspend fun generate(command: String, existing: Map<String, String>): GenerationResult {
        val lower = command.lowercase()
        if (existing.isNotEmpty()) {
            val changed = existing.toMutableMap()
            val readme = changed["README.md"].orEmpty()
            changed["README.md"] = readme + "\n\n## Alteração\n- ${command.trim()}"
            if ("modo escuro" in lower || "dark" in lower) {
                changed["app/src/main/java/generated/app/MainActivity.kt"] = sampleActivity(true)
            }
            return GenerationResult(
                "Projeto atual atualizado sem perder os arquivos existentes. Um snapshot foi criado antes da alteração.",
                changed.map { GeneratedFile(it.key, it.value) }
            )
        }
        val title = when {
            "hamburg" in lower -> "Gestão Hamburgueria"
            "cuidad" in lower -> "Gestão Cuidadora"
            "pdv" in lower -> "PDV Offline"
            "cardáp" in lower || "cardap" in lower -> "Cardápio Digital"
            else -> "Meu Aplicativo"
        }
        return GenerationResult(
            "Estrutura Android gerada localmente com Compose, navegação e persistência preparada.",
            listOf(
                GeneratedFile("README.md", "# $title\n\nGerado pelo VibeCoding AI.\n\nPedido original: $command"),
                GeneratedFile("settings.gradle.kts", "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\ndependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }\nrootProject.name=\"GeneratedApp\"\ninclude(\":app\")"),
                GeneratedFile("build.gradle.kts", "plugins { id(\"com.android.application\") version \"8.11.1\" apply false; id(\"org.jetbrains.kotlin.android\") version \"2.2.0\" apply false; id(\"org.jetbrains.kotlin.plugin.compose\") version \"2.2.0\" apply false }"),
                GeneratedFile("app/build.gradle.kts", generatedBuild()),
                GeneratedFile("app/src/main/AndroidManifest.xml", manifest(title)),
                GeneratedFile("app/src/main/java/generated/app/MainActivity.kt", sampleActivity(false))
            )
        )
    }

    private fun generatedBuild() = """
plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose") }
android { namespace="generated.app"; compileSdk=35
 defaultConfig { applicationId="generated.app"; minSdk=26; targetSdk=35; versionCode=1; versionName="1.0" }
 buildFeatures { compose=true }
}
dependencies { val bom=platform("androidx.compose:compose-bom:2026.08.00"); implementation(bom); implementation("androidx.activity:activity-compose:1.11.0"); implementation("androidx.compose.material3:material3"); implementation("androidx.compose.ui:ui") }
""".trimIndent()

    private fun manifest(title: String) = """<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:theme="@android:style/Theme.Material.Light.NoActionBar" android:label="$title"><activity android:name=".MainActivity" android:exported="true"><intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter></activity></application></manifest>"""

    private fun sampleActivity(dark: Boolean) = """
package generated.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
class MainActivity: ComponentActivity(){ override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); setContent { MaterialTheme(colorScheme=${if (dark) "darkColorScheme()" else "lightColorScheme()"}) { Surface(Modifier.fillMaxSize()) { Home() } } } } }
@Composable fun Home(){ var text by remember { mutableStateOf("") }; Column(Modifier.padding(24.dp), verticalArrangement=Arrangement.spacedBy(12.dp)){ Text("Aplicativo gerado", style=MaterialTheme.typography.headlineMedium); OutlinedTextField(text,{text=it},label={Text("Digite aqui")}); Button(onClick={ text="Salvo: "+text }){Text("Salvar")}; Text(text) } }
""".trimIndent()
}
