// Top-level build file — versions are pinned for reproducible builds.
// See SPECS.md §3 and docs/DEPENDENCIES.md before changing versions.
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
