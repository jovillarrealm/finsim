import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library)
}
kotlin {
    jvm()
    android {
        namespace = "com.finsim.shared"
        compileSdk = 37
        minSdk = 24
        withHostTestBuilder {}
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        optimization {
            consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        }
    }
    wasmJs {
        browser()
    }
    js {
        browser()
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.litertlm.android)
            implementation(libs.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
