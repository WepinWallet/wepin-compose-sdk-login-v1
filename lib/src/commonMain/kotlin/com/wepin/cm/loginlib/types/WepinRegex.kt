package com.wepin.cm.loginlib.types

import kotlinx.serialization.Serializable


class WepinRegex(regex: RegexConfig) {
    private val emailRegex: Regex = Regex(regex.email)
    private val passwordRegex: Regex = Regex(regex.password)
    private val pinRegex: Regex = Regex(regex.pin)

    fun validateEmail(email: String): Boolean {
        return emailRegex.matches(email)
    }
    fun validatePassword(password: String): Boolean {
        return passwordRegex.matches(password)
    }
    fun validatePin(pin: String): Boolean {
        return pinRegex.matches(pin)
    }
    @Serializable
    data class RegexConfig(
        val email: String,
        val password: String,
        val pin: String
    )
}
