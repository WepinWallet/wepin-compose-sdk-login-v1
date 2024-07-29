package com.wepin.cm.loginlib.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import cocoapods.JFBCrypt.*

actual fun urlEncoder(
    url: String,
    charset: String,
): String {
    if (charset !== "UTF-8") {
        throw IllegalArgumentException("Unsupported charset: $charset")
    }
    return UrlEncoder.encode(url)
}

@OptIn(ExperimentalForeignApi::class)
actual fun hashPassword(password: String): String {
    val BCRYPT_SALT = "\$2a\$10\$QCJoWqnN.acrjPIgKYCthu"
    return JFBCrypt.hashPassword(password, BCRYPT_SALT) ?: ""
}

fun customURLEncode(string: String): String {
    val allowed = NSCharacterSet.URLQueryAllowedCharacterSet.mutableCopy() as NSMutableCharacterSet
    allowed.removeCharactersInString(":/")

    val nsString = NSString.create(string = string)
    val encodedString = nsString.stringByAddingPercentEncodingWithAllowedCharacters(allowedCharacters = allowed)

    return encodedString ?: string
}
