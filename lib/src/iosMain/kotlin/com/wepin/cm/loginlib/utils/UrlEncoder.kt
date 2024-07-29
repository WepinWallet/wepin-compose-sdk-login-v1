package com.wepin.cm.loginlib.utils

object UrlEncoder {
    private val dontNeedEncoding:BooleanArray = BooleanArray(256)

    init {
        for (i in 'a'..'z') {
            dontNeedEncoding[i.code] = true
        }
        for (i in 'A'..'Z') {
            dontNeedEncoding[i.code] = true
        }
        for (i in '0'..'9') {
            dontNeedEncoding[i.code] = true
        }
        dontNeedEncoding['-'.code] = true
        dontNeedEncoding['_'.code] = true
        dontNeedEncoding['.'.code] = true
        dontNeedEncoding['*'.code] = true
    }

    fun encode(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            if (dontNeedEncoding[c.code]) {
                sb.append(c)
            } else {
                sb.append('%')
                sb.append(toHexChar(c.code shr 4 and 0xF))
                sb.append(toHexChar(c.code and 0xF))
            }
        }
        return sb.toString()
    }

    private fun toHexChar(i: Int): Char {
        return when (i) {
            in 0..9 -> ('0' + i)
            in 10..15 -> ('A' - 10 + i)
            else -> throw IllegalArgumentException("Invalid hex digit: $i")
        }
    }
}