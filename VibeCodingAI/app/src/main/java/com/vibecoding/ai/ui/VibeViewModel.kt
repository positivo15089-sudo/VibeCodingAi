package com.vibecoding.ai.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibecoding.ai.VibeCodingApplication
import com.vibecoding.ai.data.ProjectEntity
import com.vibecoding.ai.data.SnapshotEntity
import com.vibecoding.ai.domain.RuleBasedLocalEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class UiState(
    val currentProject: ProjectEntity? = null,
    val prompt: String = "",
    val files: Map<String, String> = emptyMap(),
    val selectedFile: String? = null,
    val editorText: String = "",
    val busy: Boolean = false,
    val status: String = "Pronto",
    val tab: String = "CHAT"
)

class VibeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as VibeCodingApplication).repository
    private val engine = RuleBasedLocalEngine()
    val projects = repo.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()
    val messages = _state.map { it.currentProject?.id }.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeMessages(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val snapshots = _state.map { it.currentProject?.id }.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeSnapshots(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPrompt(v: String) { _state.update { it.copy(prompt = v) } }
    fun setTab(v: String) { _state.update { it.copy(tab = v) } }
    fun setEditor(v: String) { _state.update { it.copy(editorText = v) } }

    fun createAndGenerate(prompt: String) = viewModelScope.launch {
        _state.update { it.copy(busy = true, status = "Planejando projeto...") }
        runCatching {
            val p = repo.createProject(nameFrom(prompt), prompt)
            _state.update { it.copy(currentProject = p, prompt = "", status = "Criando arquivos...") }
            repo.generate(p.id, prompt, engine)
            refreshFiles()
        }.onFailure { e -> _state.update { it.copy(status = "Erro: ${e.message}") } }
        _state.update { it.copy(busy = false, status = if (it.status.startsWith("Erro")) it.status else "Projeto pronto") }
    }

    fun openProject(project: ProjectEntity) = viewModelScope.launch {
        _state.update { it.copy(currentProject = project, tab = "CHAT", status = "Projeto aberto") }; refreshFiles()
    }

    fun sendCommand() = viewModelScope.launch {
        val p = _state.value.currentProject ?: return@launch
        val command = _state.value.prompt.trim(); if (command.isEmpty()) return@launch
        _state.update { it.copy(busy = true, prompt = "", status = "Validando alteração...") }
        runCatching { repo.generate(p.id, command, engine); refreshFiles() }
            .onFailure { e -> _state.update { it.copy(status = "Erro: ${e.message}") } }
        _state.update { it.copy(busy = false, status = if (it.status.startsWith("Erro")) it.status else "Alteração aplicada") }
    }

    fun selectFile(path: String) { _state.update { it.copy(selectedFile = path, editorText = it.files[path].orEmpty(), tab = "CÓDIGO") } }
    fun saveFile() = viewModelScope.launch {
        val s = _state.value; val p = s.currentProject ?: return@launch; val path = s.selectedFile ?: return@launch
        repo.createSnapshot(p.id, "Antes de editar $path"); repo.writeFile(p.id, path, s.editorText); refreshFiles(); _state.update { it.copy(status = "Arquivo salvo") }
    }
    fun restore(snapshot: SnapshotEntity) = viewModelScope.launch { _state.value.currentProject?.let { repo.restoreSnapshot(it.id, snapshot); refreshFiles(); _state.update { s -> s.copy(status = "Versão restaurada") } } }
    fun exportProject(onReady: (File)->Unit) = viewModelScope.launch {
        val p = _state.value.currentProject ?: return@launch
        val out = File(getApplication<Application>().cacheDir, "${p.name.replace(Regex("[^A-Za-z0-9_-]"), "_")}.zip")
        runCatching { repo.exportZip(p.id, out) }.onSuccess(onReady).onFailure { _state.update { s -> s.copy(status = "Erro ao exportar: ${it.message}") } }
    }

    fun importZip(file: File) = viewModelScope.launch {
        _state.update { it.copy(busy = true, status = "Validando ZIP...") }
        runCatching { repo.importZip(file, file.nameWithoutExtension.ifBlank { "Projeto importado" }) }
            .onSuccess { p -> _state.update { it.copy(currentProject = p, tab = "ARQUIVOS", status = "Projeto importado") }; refreshFiles() }
            .onFailure { e -> _state.update { it.copy(status = "ZIP rejeitado: ${e.message}") } }
        _state.update { it.copy(busy = false) }
    }

    fun attachFile(file: File, displayName: String) = viewModelScope.launch {
        val p = _state.value.currentProject ?: return@launch
        val safe = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val bytes = file.readBytes()
        if (bytes.size > 10 * 1024 * 1024) { _state.update { it.copy(status = "Anexo maior que 10 MB") }; return@launch }
        val target = File(repo.projectDir(p.id), "attachments/$safe")
        target.parentFile?.mkdirs(); bytes.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        refreshFiles(); _state.update { it.copy(status = "Anexo adicionado: $safe") }
    }

    fun deleteCurrent() = viewModelScope.launch { _state.value.currentProject?.let { repo.delete(it.id) }; _state.value = UiState() }

    private suspend fun refreshFiles() {
        val p = _state.value.currentProject ?: return
        val f = repo.readAllFiles(p.id); _state.update { it.copy(files = f, selectedFile = it.selectedFile?.takeIf(f::containsKey)) }
    }
    private fun nameFrom(prompt: String): String = when {
        prompt.contains("hamburg", true) -> "Gestão Hamburgueria"
        prompt.contains("cuidad", true) -> "Gestão Cuidadora"
        prompt.contains("pdv", true) -> "PDV Offline"
        prompt.contains("cardáp", true) || prompt.contains("cardap", true) -> "Cardápio Digital"
        else -> prompt.take(34).ifBlank { "Novo projeto" }
    }
}
