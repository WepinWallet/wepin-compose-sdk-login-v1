plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
//    kotlin("native.cocoapods")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

//    cocoapods {
//        summary = "Some description for a Kotlin/Native module"
//        homepage = "Link to a Kotlin/Native module homepage"
//        ios.deploymentTarget = "13.0"
//        version = "0.0.1"
//
//        pod("AppAuth") {
//            version = "~> 1.7.5"
//        }
//
//        pod("secp256k1") {
//            version = "~> 0.1.0"
//        }
//
//        pod("JFBCrypt") {
//            version = "~> 0.1"
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
//                implementation("io.wepin:wepin-compose-sdk-login-v1:0.0.2")
                api(project(":lib"))
//                api("io.wepin:wepin-compose-sdk-login-v1:0.0.2")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.10.1")
                implementation("com.google.android.gms:play-services-auth:19.2.0")
                implementation("com.google.android.material:material:1.12.0")
                implementation("net.openid:appauth:0.11.1")
            }
        }
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.wepin.loginlibrary.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()

        manifestPlaceholders["appAuthRedirectScheme"] = "wepin.d91d31ce2562adfa7bdceb31986ee270"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
