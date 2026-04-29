package com.example.simplekeystoredemo

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

sealed interface MainUiState {
    object InitialState : MainUiState
    object KeyGeneratedState : MainUiState
}

sealed interface MainUiEvent {
    object KeySuccessfullyGenerated : MainUiEvent
    object KeySuccessfullyAccessed : MainUiEvent
    object KeyDeleted : MainUiEvent
    data class Error(val message: String) : MainUiEvent
}

class MainViewModel: ViewModel() {

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.InitialState)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<MainUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var ivUsedForEncryption: ByteArray? = null

    private val keystore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    fun generateNewKey(unlockedDeviceRequired: Boolean) = viewModelScope.launch {
        val cipher = createCipher()
        val secretKey = generateKey(KEY_ALIAS, unlockedDeviceRequired)
        if (cipher != null) {
            initCipher(cipher, secretKey, Cipher.ENCRYPT_MODE)
            ivUsedForEncryption = cipher.iv
        }
        _uiState.value = MainUiState.KeyGeneratedState
        _uiEvent.send(MainUiEvent.KeySuccessfullyGenerated)
    }

    fun tryUsingKey() = viewModelScope.launch {
        val cipher = createCipher()
        val secretKey = getSecretKey(KEY_ALIAS)
        if (cipher != null && secretKey != null && ivUsedForEncryption != null) {
            initCipher(cipher, secretKey, Cipher.DECRYPT_MODE, IvParameterSpec(ivUsedForEncryption))
            _uiEvent.send(MainUiEvent.KeySuccessfullyAccessed)
        }
    }

    fun deleteKey() = viewModelScope.launch {
        deleteSecretKey(KEY_ALIAS)
        _uiState.value = MainUiState.InitialState
        _uiEvent.send(MainUiEvent.KeyDeleted)
    }

    private fun createKeyGenParameterSpec(
        alias: String,
        unlockedDeviceRequired: Boolean
    ) = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(BLOCK_MODE)
        .setEncryptionPaddings(ENCRYPTION_PADDING)
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setUnlockedDeviceRequired(unlockedDeviceRequired)
            }
        }
        .setRandomizedEncryptionRequired(true)
        .build()

    private fun generateKey(
        alias: String,
        unlockedDeviceRequired: Boolean
    ): SecretKey =
        KeyGenerator.getInstance(KEY_ALGORITHM).run {
            val keyGenParameterSpec = createKeyGenParameterSpec(alias, unlockedDeviceRequired)
            init(keyGenParameterSpec)
            generateKey()
        }

    private fun getSecretKey(alias: String) = keystore.getKey(alias, null) as? SecretKey

    private fun deleteSecretKey(alias: String) = keystore.deleteEntry(alias)

    private fun createCipher(): Cipher? {
        try {
            return Cipher.getInstance(TRANSFORMATION)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: NoSuchPaddingException) {
            throw RuntimeException("Failed to create Cipher", ex)
        }
    }

    private fun initCipher(
        cipher: Cipher,
        secretKey: SecretKey,
        operationMode: Int,
        ivParameterSpec: IvParameterSpec? = null
    ): Boolean {
        try {
            if (ivParameterSpec != null) {
                cipher.init(operationMode, secretKey, ivParameterSpec)
            } else {
                cipher.init(operationMode, secretKey)
            }
            return true
        } catch (_: KeyPermanentlyInvalidatedException) {
            return false
        } catch (ex: KeyStoreException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: UnrecoverableKeyException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to create Cipher", ex)
        }
    }

    companion object {
        private const val KEY_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val TRANSFORMATION = "$KEY_ALGORITHM/$BLOCK_MODE/$ENCRYPTION_PADDING"

        const val KEY_ALIAS = "key_alias"
    }

}
