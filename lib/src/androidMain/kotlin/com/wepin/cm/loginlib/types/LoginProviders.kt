package com.wepin.cm.loginlib.types
import com.google.gson.annotations.SerializedName

actual enum class PlatformType {
    @SerializedName("naver")
    NAVER,

    @SerializedName("discord")
    DISCORD,
}
