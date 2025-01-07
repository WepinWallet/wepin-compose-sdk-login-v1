plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
                implementation("com.google.android.gms:play-services-auth:19.2.0")
                implementation("com.google.android.material:material:1.12.0")
                implementation("net.openid:appauth:0.11.1")
                implementation(project(":sample:shared"))
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.wepin.loginlibrary"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.wepin.loginlibrary"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"

        //manifestPlaceholders["appAuthRedirectScheme"] = "wepin.d91d31ce2562adfa7bdceb31986ee270"
        manifestPlaceholders["appAuthRedirectScheme"] = "wepin.6bf47fc3fbebd80d2792e359e0480f4c"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
