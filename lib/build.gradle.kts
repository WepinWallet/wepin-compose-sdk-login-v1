import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    kotlin("plugin.serialization")
    kotlin("native.cocoapods")
}

version = "1.0.0"

val buildConfigGenerator by tasks.registering(Sync::class) {
    from(
        resources.text.fromString(
            """
        |package com.wepin.cm.loginlib
        |
        |object BuildConfig {
        |  const val PROJECT_VERSION = "${project.version}"
        |}
        |
      """.trimMargin()
        )
    ) {
        rename { "BuildConfig.kt" } // set the file name
        into("com/wepin/cm/loginlib") // change the directory to match the package
    }

    into(layout.buildDirectory.dir("generated-src/kotlin"))
}


kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "lib"
            isStatic = true
        }
    }

    cocoapods {
        summary = "Wepin Login Library"
        homepage = "https://github.com/WepinWallet/wepin-compose-sdk-login-v1"
        version = "${project.version}"

        ios.deploymentTarget = "13.0"

        extraSpecAttributes["pod_target_xcconfig"] = """{
            'KOTLIN_PROJECT_PATH' => ':lib',
            'PRODUCT_MODULE_NAME' => 'lib',
            'FRAMEWORK_SEARCH_PATHS' => '${'$'}(inherited) ${'$'}{PODS_ROOT}/AppAuth'
        }""".trimMargin()

        pod("AppAuth") {
            version = "~> 1.7.5"
        }

        pod("secp256k1") {
            version = "~> 0.1.0"
        }

        pod("JFBCrypt") {
            version = "~> 0.1"
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir(buildConfigGenerator.map { it.destinationDir })
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)

                // ECDSA
                implementation("org.bitcoinj:bitcoinj-core:0.15.10")

                // ktor
                implementation("io.ktor:ktor-client-core:2.3.11")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("io.ktor:ktor-client-logging:2.3.11")

                // encoding
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("com.google.android.material:material:1.9.0")
                // Encoding
                implementation("com.google.code.gson:gson:2.9.1")

                implementation("io.ktor:ktor-client-okhttp:2.3.11")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
                implementation("io.ktor:ktor-client-logging:2.3.11")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
                // Volley
                implementation("com.android.volley:volley:1.2.1")

                implementation("androidx.security:security-crypto-ktx:1.1.0-alpha03")
                // becrypt
                implementation("org.mindrot:jbcrypt:0.4")

                implementation("androidx.activity:activity-ktx:1.2.0-alpha05")
                implementation("androidx.fragment:fragment-ktx:1.3.0-alpha05")

                // AppAuth
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

            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.11")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.11")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.11")
                implementation("com.russhwolf:multiplatform-settings-serialization:1.1.1")
            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.wepin.cm.loginlib"

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

mavenPublishing {
//    publishToMavenCentral(SonatypeHost.DEFAULT)
    // or when publishing to https://s01.oss.sonatype.org
//    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    signAllPublications()
    coordinates("io.wepin", "wepin-compose-sdk-login-v1", "${project.version}")

    pom {
        name.set(project.name)
        description.set("compose multiplatform login library for Wepin SDK")
        inceptionYear.set("2024")
        url.set("https://github.com/WepinWallet/wepin-compose-sdk-login-v1")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("IoTrust")
                name.set("wepin.dev")
                url.set("https://github.com/WepinWallet/wepin-compose-sdk-login-v1")
            }
        }
        scm {
            url.set("https://github.com/WepinWallet/wepin-compose-sdk-login-v1/")
            connection.set("scm:git:git://github.com/WepinWallet/wepin-compose-sdk-login-v1")
            developerConnection.set("scm:git:ssh://git@github.com/WepinWallet/wepin-compose-sdk-login-v1")
        }
    }
}
