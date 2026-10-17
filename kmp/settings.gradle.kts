pluginManagement {
    // Kotlin 2.2 metadata needs a newer R8 than the one bundled with AGP 8.7.3.
    buildscript {
        repositories {
            google()
            mavenCentral()
        }
        dependencies {
            classpath("com.android.tools:r8:8.10.21")
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "finsim"
include(":shared")
include(":composeApp")
