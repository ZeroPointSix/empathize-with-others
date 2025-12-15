package com.empathy.ai.domain.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据加密工具类
 *
 * 提供AES/GCM加密解密功能，密钥存储在AndroidKeyStore中
 *
 * 职责：
 * - 生成和管理加密密钥
 * - 加密敏感数据
 * - 解密数据
 * - 安全删除密钥
 *
 * 使用示例：
 * ```kotlin
 * val encrypted = dataEncryption.encrypt("sensitive data")
 * val decrypted = dataEncryption.decrypt(encrypted)
 * ```
 */
@Singleton
class DataEncryption @Inject constructor() {
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(SecurityConfig.KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }
    
    /**
     * 加密字符串数据
     *
     * @param plainText 明文数据
     * @param keyAlias 密钥别名，默认使用数据加密密钥
     * @return Base64编码的加密数据（包含IV）
     */
    fun encrypt(
        plainText: String,
        keyAlias: String = SecurityConfig.DATA_KEY_ALIAS
    ): Result<String> {
        return try {
            val secretKey = getOrCreateKey(keyAlias)
            val cipher = Cipher.getInstance(SecurityConfig.ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // 将IV和密文组合：IV(12字节) + 密文
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Result.success(Base64.encodeToString(combined, Base64.NO_WRAP))
        } catch (e: Exception) {
            Result.failure(EncryptionException("加密失败: ${e.message}", e))
        }
    }
    
    /**
     * 解密字符串数据
     *
     * @param encryptedData Base64编码的加密数据
     * @param keyAlias 密钥别名
     * @return 解密后的明文
     */
    fun decrypt(
        encryptedData: String,
        keyAlias: String = SecurityConfig.DATA_KEY_ALIAS
    ): Result<String> {
        return try {
            val combined = Base64.decode(encryptedData, Base64.NO_WRAP)
            
            // 提取IV和密文
            val iv = combined.copyOfRange(0, SecurityConfig.IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(SecurityConfig.IV_LENGTH, combined.size)
            
            val secretKey = getKey(keyAlias)
                ?: return Result.failure(EncryptionException("密钥不存在: $keyAlias"))
            
            val cipher = Cipher.getInstance(SecurityConfig.ENCRYPTION_ALGORITHM)
            val spec = GCMParameterSpec(SecurityConfig.GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            Result.success(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(EncryptionException("解密失败: ${e.message}", e))
        }
    }
    
    /**
     * 加密字节数组
     */
    fun encryptBytes(
        data: ByteArray,
        keyAlias: String = SecurityConfig.DATA_KEY_ALIAS
    ): Result<ByteArray> {
        return try {
            val secretKey = getOrCreateKey(keyAlias)
            val cipher = Cipher.getInstance(SecurityConfig.ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data)
            
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Result.success(combined)
        } catch (e: Exception) {
            Result.failure(EncryptionException("加密失败: ${e.message}", e))
        }
    }
    
    /**
     * 解密字节数组
     */
    fun decryptBytes(
        encryptedData: ByteArray,
        keyAlias: String = SecurityConfig.DATA_KEY_ALIAS
    ): Result<ByteArray> {
        return try {
            val iv = encryptedData.copyOfRange(0, SecurityConfig.IV_LENGTH)
            val encryptedBytes = encryptedData.copyOfRange(SecurityConfig.IV_LENGTH, encryptedData.size)
            
            val secretKey = getKey(keyAlias)
                ?: return Result.failure(EncryptionException("密钥不存在: $keyAlias"))
            
            val cipher = Cipher.getInstance(SecurityConfig.ENCRYPTION_ALGORITHM)
            val spec = GCMParameterSpec(SecurityConfig.GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            Result.success(cipher.doFinal(encryptedBytes))
        } catch (e: Exception) {
            Result.failure(EncryptionException("解密失败: ${e.message}", e))
        }
    }
    
    /**
     * 检查密钥是否存在
     */
    fun hasKey(keyAlias: String): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 删除密钥
     *
     * ⚠️ 警告：删除密钥后，使用该密钥加密的数据将无法解密
     */
    fun deleteKey(keyAlias: String): Result<Unit> {
        return try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(EncryptionException("删除密钥失败: ${e.message}", e))
        }
    }
    
    /**
     * 获取或创建密钥
     */
    private fun getOrCreateKey(keyAlias: String): SecretKey {
        return getKey(keyAlias) ?: createKey(keyAlias)
    }
    
    /**
     * 获取已存在的密钥
     */
    private fun getKey(keyAlias: String): SecretKey? {
        return try {
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 创建新密钥
     */
    private fun createKey(keyAlias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            SecurityConfig.KEY_ALGORITHM,
            SecurityConfig.KEYSTORE_PROVIDER
        )
        
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(SecurityConfig.KEY_SIZE)
            .setUserAuthenticationRequired(SecurityConfig.REQUIRE_USER_AUTHENTICATION)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}

/**
 * 加密异常
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
