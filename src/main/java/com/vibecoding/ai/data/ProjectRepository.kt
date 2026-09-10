package com.vibecoding.ai.data

import com.vibecoding.ai.domain.AiEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ProjectRepository(private val dao: ProjectDao, private val appFilesDir: File) {
    private val projectsRoot = File(appFilesDir, "projects").apply { mkdirs() }

    fun observeProjects(): Flow<List<ProjectEntity>> = dao.observeProjects()
    fun observeMessages(projectId: String) = dao.observeMessages(projectId)
    fun observeSnapshots(projectId: String) = dao.observeSnapshots(projectId)

    suspend fun createProject(name: String, description: String): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(UUID.randomUUID().toString(), name.ifBlank { "Novo projeto" }, description, now, now)
        dao.upsertProject(project)
        projectDir(project.id).mkdirs()
        return project
    }

    suspend fun rename(id: String, newName: String) {
        dao.getProject(id)?.let { dao.upsertProject(it.copy(name = newName, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        projectDir(id).deleteRecursively(); dao.deleteMessages(id); dao.deleteSnapshots(id); dao.deleteProject(id)
    }

    suspend fun generate(projectId: String, command: String, engine: AiEngine): String = withContext(Dispatchers.IO) {
        val project = dao.getProject(projectId) ?: error("Projeto não encontrado")
        val before = readAllFiles(projectId)
        if (before.isNotEmpty()) createSnapshot(projectId, "Antes: ${command.take(40)}")
        dao.insertMessage(MessageEntity(projectId = projectId, role = "user", content = command, createdAt = System.currentTimeMillis()))
        val result = engine.generate(command, before)
        writeFiles(projectId, result.files.associate { it.path to it.content })
        dao.insertMessage(MessageEntity(projectId = projectId, role = "assistant", content = result.summary, createdAt = System.currentTimeMillis()))
        dao.upsertProject(project.copy(description = command.take(160), updatedAt = System.currentTimeMillis()))
        result.summary
    }

    fun projectDir(projectId: String) = File(projectsRoot, projectId)

    suspend fun readAllFiles(projectId: String): Map<String, String> = withContext(Dispatchers.IO) {
        val root = projectDir(projectId)
        if (!root.exists()) emptyMap() else root.walkTopDown().filter { it.isFile && it.length() <= 2_000_000 }.associate { f ->
            f.relativeTo(root).invariantSeparatorsPath to runCatching { f.readText() }.getOrDefault("<arquivo binário>")
        }
    }

    suspend fun writeFile(projectId: String, relativePath: String, content: String) = withContext(Dispatchers.IO) {
        val root = projectDir(projectId).canonicalFile
        val target = File(root, relativePath).canonicalFile
        require(target.path.startsWith(root.path + File.separator)) { "Caminho inválido" }
        target.parentFile?.mkdirs(); target.writeText(content)
    }

    private fun writeFiles(projectId: String, files: Map<String, String>) {
        val root = projectDir(projectId).canonicalFile.apply { mkdirs() }
        files.forEach { (path, content) ->
            val target = File(root, path).canonicalFile
            if (target.path.startsWith(root.path + File.separator)) {
                target.parentFile?.mkdirs(); target.writeText(content)
            }
        }
    }

    suspend fun createSnapshot(projectId: String, label: String) = withContext(Dispatchers.IO) {
        val snapDir = File(appFilesDir, "snapshots/$projectId").apply { mkdirs() }
        val zip = File(snapDir, "${System.currentTimeMillis()}.zip")
        zipDirectory(projectDir(projectId), zip)
        dao.insertSnapshot(SnapshotEntity(projectId = projectId, label = label, createdAt = System.currentTimeMillis(), archivePath = zip.absolutePath))
    }

    suspend fun restoreSnapshot(projectId: String, snapshot: SnapshotEntity) = withContext(Dispatchers.IO) {
        val root = projectDir(projectId)
        root.deleteRecursively(); root.mkdirs()
        unzipSafe(File(snapshot.archivePath), root)
    }

    suspend fun exportZip(projectId: String, destination: File): File = withContext(Dispatchers.IO) {
        zipDirectory(projectDir(projectId), destination); destination
    }

    suspend fun importZip(zip: File, name: String): ProjectEntity = withContext(Dispatchers.IO) {
        val p = createProject(name, "Projeto importado")
        unzipSafe(zip, projectDir(p.id)); p
    }

    private fun zipDirectory(source: File, out: File) {
        out.parentFile?.mkdirs()
        ZipOutputStream(out.outputStream().buffered()).use { zos ->
            if (!source.exists()) return@use
            source.walkTopDown().filter { it.isFile }.forEach { file ->
                val entry = ZipEntry(file.relativeTo(source).invariantSeparatorsPath)
                zos.putNextEntry(entry); file.inputStream().use { it.copyTo(zos) }; zos.closeEntry()
            }
        }
    }

    private fun unzipSafe(zip: File, destination: File) {
        val root = destination.canonicalFile
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val target = File(root, entry.name).canonicalFile
                require(target.path.startsWith(root.path + File.separator)) { "ZIP contém caminho inseguro" }
                if (entry.isDirectory) target.mkdirs() else { target.parentFile?.mkdirs(); target.outputStream().use { zis.copyTo(it) } }
                zis.closeEntry(); entry = zis.nextEntry
            }
        }
    }
}
