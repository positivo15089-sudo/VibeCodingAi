package com.vibecoding.ai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vibecoding.ai.ui.VibeViewModel
import com.vibecoding.ai.ui.theme.VibeTheme

class MainActivity : ComponentActivity() {
    private val vm: VibeViewModel by viewModels()

    private val importZip = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val temp = java.io.File(cacheDir, "import_${System.currentTimeMillis()}.zip")
        contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } }
        vm.importZip(temp)
    }

    private val attachAny = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "anexo_${System.currentTimeMillis()}"
        val temp = java.io.File(cacheDir, "attach_${System.currentTimeMillis()}")
        contentResolver.openInputStream(uri)?.use { input -> temp.outputStream().use { input.copyTo(it) } }
        vm.attachFile(temp, name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeTheme {
                Surface(Modifier.fillMaxSize()) {
                    App(vm,
                        onShare = { file ->
                            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }, "Exportar projeto"))
                        },
                        onImport = { importZip.launch("application/zip") },
                        onAttach = { attachAny.launch("*/*") }
                    )
                }
            }
        }
    }
}

@Composable
private fun App(vm: VibeViewModel, onShare: (java.io.File)->Unit, onImport: ()->Unit, onAttach: ()->Unit) {
    val state by vm.state.collectAsState()
    val projects by vm.projects.collectAsState()
    val messages by vm.messages.collectAsState()
    val snapshots by vm.snapshots.collectAsState()

    if (state.currentProject == null) {
        HomeScreen(
            prompt = state.prompt,
            projects = projects,
            busy = state.busy,
            status = state.status,
            onPrompt = vm::setPrompt,
            onCreate = { vm.createAndGenerate(state.prompt) },
            onOpen = vm::openProject,
            onImport = onImport,
            onAttach = onAttach
        )
    } else {
        ProjectScreen(
            state = state,
            messages = messages,
            snapshots = snapshots,
            onPrompt = vm::setPrompt,
            onSend = vm::sendCommand,
            onTab = vm::setTab,
            onSelectFile = vm::selectFile,
            onEditor = vm::setEditor,
            onSave = vm::saveFile,
            onRestore = vm::restore,
            onExport = { vm.exportProject(onShare) },
            onDelete = vm::deleteCurrent,
            onAttach = onAttach
        )
    }
}

@Composable
private fun HomeScreen(
    prompt: String,
    projects: List<com.vibecoding.ai.data.ProjectEntity>,
    busy: Boolean,
    status: String,
    onPrompt: (String)->Unit,
    onCreate: ()->Unit,
    onOpen: (com.vibecoding.ai.data.ProjectEntity)->Unit,
    onImport: ()->Unit,
    onAttach: ()->Unit
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 52.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.padding(14.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column { Text("VibeCoding AI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Transforme uma ideia em aplicativo.") }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPrompt,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    label = { Text("Descreva o aplicativo que você quer criar...") }
                )
                Button(onClick = onCreate, enabled = prompt.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.RocketLaunch, null); Spacer(Modifier.width(8.dp)); Text("CRIAR APP")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAttach, modifier = Modifier.weight(1f), enabled = false) { Icon(Icons.Default.AttachFile, null); Text(" IMAGEM") }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.FolderOpen, null); Text(" IMPORTAR") }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("Projetos recentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(projects, key = { it.id }) { p ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { onOpen(p) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Android, null); Spacer(Modifier.width(12.dp));
                        Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text(p.description, maxLines = 1, style = MaterialTheme.typography.bodySmall) }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectScreen(
    state: com.vibecoding.ai.ui.UiState,
    messages: List<com.vibecoding.ai.data.MessageEntity>,
    snapshots: List<com.vibecoding.ai.data.SnapshotEntity>,
    onPrompt: (String)->Unit, onSend: ()->Unit, onTab: (String)->Unit,
    onSelectFile: (String)->Unit, onEditor: (String)->Unit, onSave: ()->Unit,
    onRestore: (com.vibecoding.ai.data.SnapshotEntity)->Unit,
    onExport: ()->Unit, onDelete: ()->Unit, onAttach: ()->Unit
) {
    val tabs = listOf("CHAT", "PRÉVIA", "CÓDIGO", "ARQUIVOS", "BUILD")
    Column(Modifier.fillMaxSize().padding(top = 40.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(state.currentProject?.name.orEmpty(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(state.status, style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = onExport) { Icon(Icons.Default.IosShare, "Exportar ZIP") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Excluir") }
        }
        ScrollableTabRow(selectedTabIndex = tabs.indexOf(state.tab).coerceAtLeast(0), edgePadding = 8.dp) {
            tabs.forEach { t -> Tab(selected = state.tab == t, onClick = { onTab(t) }, text = { Text(t) }) }
        }
        when (state.tab) {
            "CHAT" -> ChatTab(messages, state.prompt, state.busy, onPrompt, onSend, onAttach)
            "PRÉVIA" -> PreviewTab(state.files)
            "CÓDIGO" -> CodeTab(state, onEditor, onSave)
            "ARQUIVOS" -> FilesTab(state.files.keys.toList(), onSelectFile)
            "BUILD" -> BuildTab(snapshots, onRestore, onExport)
        }
    }
}

@Composable
private fun ChatTab(messages: List<com.vibecoding.ai.data.MessageEntity>, prompt: String, busy: Boolean, onPrompt: (String)->Unit, onSend: ()->Unit, onAttach: ()->Unit) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { m ->
                Surface(color = if (m.role == "user") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(12.dp)) { Text(if (m.role == "user") "Você" else "VibeCoding AI", fontWeight = FontWeight.Bold); Text(m.content) }
                }
            }
        }
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Bottom) {
            IconButton(onClick = onAttach) { Icon(Icons.Default.AttachFile, "Anexar") }
            OutlinedTextField(prompt, onPrompt, Modifier.weight(1f), placeholder = { Text("O que você quer alterar?") })
            IconButton(onClick = onSend, enabled = prompt.isNotBlank() && !busy) { Icon(Icons.Default.Send, "Enviar") }
        }
    }
}

@Composable
private fun FilesTab(paths: List<String>, onSelect: (String)->Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(paths.sorted()) { path -> ListItem(headlineContent = { Text(path) }, leadingContent = { Icon(Icons.Default.Description, null) }, modifier = Modifier.clickable { onSelect(path) }) }
    }
}

@Composable
private fun CodeTab(state: com.vibecoding.ai.ui.UiState, onEditor: (String)->Unit, onSave: ()->Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(state.selectedFile ?: "Selecione um arquivo em ARQUIVOS", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.editorText, onValueChange = onEditor, modifier = Modifier.fillMaxWidth().weight(1f), textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace))
        Button(onClick = onSave, enabled = state.selectedFile != null, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Icon(Icons.Default.Save, null); Text(" SALVAR ARQUIVO") }
    }
}

@Composable
private fun PreviewTab(files: Map<String,String>) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Prévia estrutural", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.widthIn(max = 360.dp).fillMaxWidth().weight(1f), tonalElevation = 4.dp, shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Aplicativo gerado", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${files.size} arquivos no projeto")
                OutlinedTextField("", {}, label = { Text("Campo de exemplo") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Botão funcional na prévia") }
                Text("A prévia nativa completa depende de compilar/carregar o app gerado; esta tela não finge executar bytecode do projeto.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BuildTab(snapshots: List<com.vibecoding.ai.data.SnapshotEntity>, onRestore: (com.vibecoding.ai.data.SnapshotEntity)->Unit, onExport: ()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Gerar aplicativo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Button(onClick = onExport, Modifier.fillMaxWidth()) { Icon(Icons.Default.Archive, null); Text(" EXPORTAR PROJETO ZIP") }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("GERAR APK DEBUG — requer toolchain Gradle/SDK") }
        OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("GERAR APK RELEASE — requer assinatura/toolchain") }
        Text("No Android comum, o app não executa Gradle arbitrário por segurança. O ZIP exportado inclui estrutura pronta para build externo/controlado.")
        HorizontalDivider()
        Text("Versões", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        snapshots.forEachIndexed { i, s ->
            ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Versão ${snapshots.size-i}", fontWeight = FontWeight.Bold); Text(s.label, style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { onRestore(s) }) { Text("Restaurar") } } }
        }
    }
}
