// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ZipVaultTest {
    @get:Rule val folder = TemporaryFolder()

    private fun vault(password: String = "senha-forte-123") =
        ZipVault(folder.root.resolve("vault.zip"), password.toCharArray())

    @Test fun createdVault_verifiesWithRightPassword() {
        vault().create()
        assertTrue(vault().verify())
    }

    @Test fun wrongPassword_doesNotVerify() {
        vault().create()
        assertFalse(vault("outra-senha-123").verify())
    }

    @Test fun addedFile_isListedAndReadBack() {
        val v = vault()
        v.create()
        val bytes = ByteArray(5000) { (it % 251).toByte() }
        v.add("Pictures/Trip/a.jpg", bytes.inputStream(), 1_700_000_000_000L)
        val entries = vault().list()
        assertEquals(listOf("Pictures/Trip/a.jpg"), entries.map { it.name })
        assertEquals(5000L, entries.single().sizeBytes)
        assertArrayEquals(bytes, vault().open("Pictures/Trip/a.jpg").use { it.readBytes() })
    }

    @Test fun markerIsNotListed() {
        val v = vault()
        v.create()
        assertTrue(v.list().isEmpty())
    }

    @Test fun removedFile_disappears() {
        val v = vault()
        v.create()
        v.add("a.jpg", ByteArray(10).inputStream(), 0L)
        v.add("b.jpg", ByteArray(10).inputStream(), 0L)
        v.remove("a.jpg")
        assertEquals(listOf("b.jpg"), v.list().map { it.name })
        assertTrue(v.verify())
    }

    @Test fun fileIsNotStoredInPlainText() {
        val v = vault()
        v.create()
        val secret = "SEGREDO-MUITO-ESPECIFICO-12345".repeat(20)
        v.add("nota.txt", secret.toByteArray().inputStream(), 0L)
        val raw = folder.root.resolve("vault.zip").readBytes()
        assertFalse(String(raw, Charsets.ISO_8859_1).contains("SEGREDO-MUITO-ESPECIFICO"))
    }

    @Test fun uniqueName_addsCounter() {
        assertEquals("a.jpg", uniqueEntryName(emptySet(), "a.jpg"))
        assertEquals("a (1).jpg", uniqueEntryName(setOf("a.jpg"), "a.jpg"))
        assertEquals("a (2).jpg", uniqueEntryName(setOf("a.jpg", "a (1).jpg"), "a.jpg"))
        assertEquals("Pictures/x/a (1)", uniqueEntryName(setOf("Pictures/x/a"), "Pictures/x/a"))
    }

    @Test fun entry_parsesNameAndType() {
        val e = VaultEntry("Movies/Trip/clip.MP4", 1, 0)
        assertEquals("clip.MP4", e.displayName)
        assertEquals("Movies/Trip/", e.folder)
        assertTrue(e.isVideo)
        assertFalse(VaultEntry("a.png", 1, 0).isVideo)
    }
}
