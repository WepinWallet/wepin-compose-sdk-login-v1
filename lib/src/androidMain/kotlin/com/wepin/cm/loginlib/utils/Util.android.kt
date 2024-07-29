package com.wepin.cm.loginlib.utils

import org.mindrot.jbcrypt.BCrypt
import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

actual fun hashPassword(password: String): String {
    val BCRYPT_SALT = "\$2a\$10\$QCJoWqnN.acrjPIgKYCthu"
    return BCrypt.hashpw(password, BCRYPT_SALT)
}

actual fun urlEncoder(
    url: String,
    charset: String,
): String {
    when (charset) {
        "UTF-8" -> return URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    }
    return ""
}

fun bigIntegerToByteArrayTrimmed(value: BigInteger): ByteArray {
    val hexChars = "0123456789ABCDEF"
    var byteArray = value.toByteArray()
    if (byteArray.isNotEmpty() && byteArray[0] == 0.toByte() && byteArray[1].toInt() < 0) {
        byteArray = byteArray.copyOfRange(1, byteArray.size)
    }
    return byteArray
}
