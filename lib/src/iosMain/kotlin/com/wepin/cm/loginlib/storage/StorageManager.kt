package com.wepin.cm.loginlib.storage

import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRef
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFStringCreateFromExternalRepresentation
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecParam
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitAll
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnAttributes
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class StorageManager {
    private val service = "wepin_key_chain_v1"

    fun read(appId: String, key: String?): String? {
        return memScoped {
            val formattedKey = formatKey(appId, key)
            val query = query(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to service.toCFDict(),
                kSecAttrAccount to formattedKey.toCFDict(),
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitOne
            )

            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess && result.value != null) {
                val value = CFBridgingRelease(result.value) as? NSData
                value?.let { NSString.create(it, NSUTF8StringEncoding) as String }
            } else {
                null
            }
        }
    }

    fun write(appId: String, key: String?, data: String) {
        val formattedKey = formatKey(appId, key)
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCFDict(),
            kSecAttrAccount to formattedKey.toCFDict(),
            kSecValueData to data.toCFDict()
        )

        SecItemDelete(query)
        val status = SecItemAdd(query, null)
        if (status != errSecSuccess) {
            println("Failed to write data to keychain")
        }
    }


    fun delete(appId: String, key: String?) {
        val formattedKey = formatKey(appId, key)
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCFDict(),
            kSecAttrAccount to formattedKey.toCFDict(),
        )

        SecItemDelete(query)
    }
    fun deleteAll() {
        val query = query(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service.toCFDict(),
        )

        SecItemDelete(query)
    }
    fun readAll(appId: String): Map<String, String?> {
        return memScoped {
            val query = query(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to service.toCFDict(),
                kSecReturnAttributes to kCFBooleanTrue,
                kSecReturnData to kCFBooleanTrue,
                kSecMatchLimit to kSecMatchLimitAll
            )

            val resultMap = mutableMapOf<String, String?>()
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            if (status == errSecSuccess) {
                (result.value as? CFArrayRef)?.let { items ->
                    for (i in 0 until CFArrayGetCount(items)) {
                        (CFArrayGetValueAtIndex(items, i) as? CFDictionaryRef)?.let { dict ->
                            val account = extractStringFromCFDictionary(dict, kSecAttrAccount)
                            val dataValue = extractStringFromCFDictionary(dict, kSecValueData)

                            if (!account.isNullOrBlank() && account.startsWith(appId)) {
//                                val key = account.replace("${appId}_", "")
                                resultMap[account] = dataValue
                            }
                        }
                    }
                }
            }
            resultMap
        }
    }
    fun writeAll(appId: String, data: Map<String, String>) {
        for ((key, value) in data) {
            write(appId, key, value)
        }
    }

    private fun formatKey(appId: String, key: String?):String {
        return "${appId}_${key ?: ""}"
    }

    private fun extractStringFromCFDictionary(dict: CFDictionaryRef, key: CValuesRef<*>?): String? {
        val dataRef = CFDictionaryGetValue(dict, key) as? CFDataRef
        return dataRef?.let {
            CFBridgingRelease(CFStringCreateFromExternalRepresentation(null, it, kCFStringEncodingUTF8)) as? String
        }
    }

    private fun query(vararg pairs: Pair<CValuesRef<*>?, CValuesRef<*>?>): CFMutableDictionaryRef? {
        memScoped {
            val dict = CFDictionaryCreateMutable(null, pairs.size.toLong(), null, null)
            pairs.forEach {
                CFDictionaryAddValue(dict, it.first, it.second)
            }
            return dict
        }
    }
    private fun String.toCFDict(): CFTypeRef? {
        memScoped {
            return CFBridgingRetain(
                NSData.dataWithBytes(
                    bytes = this@toCFDict.cstr.ptr,
                    length = this@toCFDict.length.toULong()
                )
            )
        }
    }

}