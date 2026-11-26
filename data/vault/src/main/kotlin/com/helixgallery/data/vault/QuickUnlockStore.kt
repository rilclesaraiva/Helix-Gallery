// Helix Gallery — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/Helix-Gallery/blob/main/LICENSE

package com.helixgallery.data.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda a senha do cofre cifrada com uma chave do Android Keystore que só funciona depois de a
 * pessoa se identificar (biometria ou PIN/padrão/senha do aparelho, vale por 30 s). Sem isso o
 * desbloqueio rápido não existe e a senha digitada continua valendo. Só a cifra fica no app.
 */
internal class QuickUnlockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("vault_quick_unlock", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.contains(KEY_DATA)

    /** Cifra e guarda [password]. Exige identificação feita há menos de 30 s. */
    fun save(password: CharArray): Boolean = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        val bytes = String(password).toByteArray()
        val encrypted = cipher.doFinal(bytes)
        bytes.fill(0)
        prefs.edit()
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
        true
    } catch (_: Exception) {
        false
    }

    /** A senha guardada, ou null (sem identificação recente, chave invalidada, nada guardado). */
    fun load(): CharArray? {
        val iv = prefs.getString(KEY_IV, null) ?: return null
        val data = prefs.getString(KEY_DATA, null) ?: return null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(create = false) ?: return null, GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            val bytes = cipher.doFinal(Base64.decode(data, Base64.NO_WRAP))
            String(bytes).toCharArray().also { bytes.fill(0) }
        } catch (_: android.security.keystore.KeyPermanentlyInvalidatedException) {
            // Mudou a biometria ou o bloqueio de tela: a chave morreu. Volta para a senha digitada.
            clear()
            null
        } catch (_: Exception) {
            null
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
        runCatching { KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(ALIAS) }
    }

    private fun key(create: Boolean): SecretKey? {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        if (!create) return null
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    AUTH_VALID_SECONDS,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "helix_vault_quick_unlock"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_IV = "iv"
        const val KEY_DATA = "data"
        const val AUTH_VALID_SECONDS = 30
    }
}
