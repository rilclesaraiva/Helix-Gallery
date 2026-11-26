// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import android.app.KeyguardManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

internal class ZipVaultRepository(private val context: Context) : VaultRepository {
    private val storage = VaultStorage(context)
    private val quick = QuickUnlockStore(context)
    private val autoAdd = AutoAddStore(context)
    private val mutex = Mutex()

    private val workDir = File(context.cacheDir, "vault")
    private val workFile = File(workDir, "work.zip")
    private val viewDir = File(workDir, "view")

    @Volatile private var vault: ZipVault? = null
    @Volatile private var password: CharArray? = null

    override fun exists(): Boolean = storage.find() != null

    override fun isUnlocked(): Boolean = vault != null

    private fun open(pass: CharArray) {
        password = pass.copyOf()
        vault = ZipVault(workFile, pass)
        // Lembra a senha (só para guardar coisas) para trancar uma foto sem pedir de novo.
        autoAdd.save(pass)
    }

    override suspend fun create(password: CharArray) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                check(storage.find() == null) { "Vault already exists" }
                lock()
                workDir.mkdirs()
                ZipVault(workFile, password).create()
                storage.writeBack(workFile)
                open(password)
            }
        }
    }

    override suspend fun unlock(password: CharArray): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val uri = storage.find() ?: return@withLock false
            lock()
            storage.copyTo(uri, workFile)
            if (ZipVault(workFile, password).verify()) {
                open(password)
                true
            } else {
                wipeWork()
                false
            }
        }
    }

    override suspend fun addQuietly(sources: List<VaultSource>): VaultAddResult? {
        if (isUnlocked()) return add(sources)
        if (!exists()) return null
        val saved = autoAdd.load() ?: return null
        val opened = try {
            unlock(saved)
        } catch (_: Exception) {
            false
        } finally {
            saved.fill('\u0000')
        }
        if (!opened) return null
        return try {
            add(sources)
        } finally {
            // Volta a trancar: quem ver a tela não vê o cofre.
            lock()
        }
    }

    override suspend fun entries(): List<VaultEntry> = withContext(Dispatchers.IO) {
        mutex.withLock { requireVault().list().sortedByDescending { it.takenMs } }
    }

    override suspend fun add(sources: List<VaultSource>): VaultAddResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val v = requireVault()
            val names = v.list().map { it.name }.toMutableSet()
            val stored = mutableListOf<String>()
            var failed = 0
            for (source in sources) {
                try {
                    val name = uniqueEntryName(names, source.folder + source.displayName)
                    val input = context.contentResolver.openInputStream(Uri.parse(source.uri)) ?: error("Cannot open")
                    v.add(name, input, source.takenMs)
                    names += name
                    stored += source.uri
                } catch (_: Exception) {
                    failed++
                }
            }
            // Só conta como guardado depois de gravado no cofre de verdade.
            storage.writeBack(workFile)
            VaultAddResult(stored, failed)
        }
    }

    override suspend fun extractForViewing(name: String): File = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = File(viewDir, name.replace('/', '_'))
            if (!target.exists()) {
                viewDir.mkdirs()
                requireVault().open(name).use { input -> target.outputStream().use { input.copyTo(it) } }
            }
            target
        }
    }

    override suspend fun restore(names: List<String>): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            val v = requireVault()
            val byName = v.list().associateBy { it.name }
            var restored = 0
            for (name in names) {
                val entry = byName[name] ?: continue
                try {
                    insertIntoGallery(v, entry)
                    v.remove(name)
                    restored++
                } catch (_: Exception) {
                    // Não conseguiu devolver: continua no cofre.
                }
            }
            storage.writeBack(workFile)
            restored
        }
    }

    private fun insertIntoGallery(v: ZipVault, entry: VaultEntry) {
        val video = entry.isVideo
        val collection = if (video) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val allowed = if (video) listOf("DCIM/", "Movies/") else listOf("DCIM/", "Pictures/")
        val folder = entry.folder.takeIf { f -> allowed.any { f.startsWith(it) } }
            ?: if (video) "Movies/HelixGallery/" else "Pictures/HelixGallery/"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, entry.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeFor(entry.displayName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (entry.takenMs > 0) {
                put(MediaStore.MediaColumns.DATE_MODIFIED, entry.takenMs / 1000)
                if (video) put(MediaStore.Video.Media.DATE_TAKEN, entry.takenMs)
                else put(MediaStore.Images.Media.DATE_TAKEN, entry.takenMs)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: error("Cannot insert")
        try {
            resolver.openOutputStream(uri)?.use { out -> v.open(entry.name).use { it.copyTo(out) } } ?: error("Cannot write")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    override suspend fun delete(names: List<String>) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val v = requireVault()
                names.forEach { v.remove(it) }
                storage.writeBack(workFile)
            }
        }
    }

    override fun lock() {
        password?.fill('\u0000')
        password = null
        vault = null
        wipeWork()
    }

    private fun wipeWork() {
        workDir.deleteRecursively()
    }

    private fun requireVault(): ZipVault = vault ?: error("Vault is locked")

    override fun quickUnlockSupported(): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

    override fun quickUnlockEnabled(): Boolean = quick.isEnabled()

    override fun enableQuickUnlock(): Boolean = password?.let { quick.save(it) } ?: false

    override fun disableQuickUnlock() = quick.clear()

    override fun quickUnlockPassword(): CharArray? = quick.load()
}
