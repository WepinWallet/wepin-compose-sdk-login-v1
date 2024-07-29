package com.wepin.cm.loginlib.utils

import com.wepin.cm.loginlib.BuildConfig
import com.wepin.cm.loginlib.types.StorageDataType
import io.ktor.util.decodeBase64String
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun decodeBase64(base64String: String): String {
    return base64String.decodeBase64String()
}

expect fun hashPassword(password: String): String

expect fun urlEncoder(
    url: String,
    charset: String,
): String

fun convertLocalStorageDataToJson(data: StorageDataType): String {
    val json =
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    return json.encodeToString(data)
}

fun convertJsonToLocalStorageData(jsonData: String): StorageDataType {
    val json =
        Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        }
    return json.decodeFromString(jsonData)
}

fun getVersionMetaDataValue(): String {
//    try {
//        return BuildConfig.LIBRARY_VERSION
//    } catch (e: PackageManager.NameNotFoundException) {
//        e.printStackTrace()
//    }
    return BuildConfig.PROJECT_VERSION
}


