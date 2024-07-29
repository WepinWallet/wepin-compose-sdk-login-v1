plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("multiplatform").apply(false)
    id("com.android.application").apply(false)
    id("com.android.library").apply(false)
    id("org.jetbrains.compose").apply(false)
    id("com.vanniktech.maven.publish").apply(false)
    id("org.jetbrains.dokka")
    kotlin("plugin.serialization").version("1.9.10")
}

tasks.register<Copy>("setUpGitHooks") {
    group = "help"
    from("$rootDir/.hooks")
    into("$rootDir/.git/hooks")
}
