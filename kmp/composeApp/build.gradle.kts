import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

val doclingWorkerDirectory = layout.projectDirectory.dir("docling-worker")
val doclingWorkerResources = layout.buildDirectory.dir("generated/doclingResources")
val npmExecutable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) "npm.cmd" else "npm"

val installDoclingWorkerDependencies by tasks.registering(Exec::class) {
    workingDir(doclingWorkerDirectory)
    commandLine(npmExecutable, "ci", "--no-audit", "--no-fund")
    inputs.files(doclingWorkerDirectory.file("package.json"), doclingWorkerDirectory.file("package-lock.json"))
    outputs.dir(doclingWorkerDirectory.dir("node_modules"))
}

val buildDoclingWorker by tasks.registering(Exec::class) {
    dependsOn(installDoclingWorkerDependencies)
    workingDir(doclingWorkerDirectory)
    commandLine(npmExecutable, "run", "build")
    inputs.files(
        fileTree(doclingWorkerDirectory.dir("src")),
        doclingWorkerDirectory.file("build.mjs"),
        doclingWorkerDirectory.file("package.json"),
        doclingWorkerDirectory.file("package-lock.json")
    )
    outputs.dir(doclingWorkerResources)
}

kotlin {
    android {
        namespace = "com.finsim.compose"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        withHostTestBuilder {}
        androidResources {
            enable = true
        }
    }
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.kermit)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.named("wasmJsBrowserDistribution") {
    dependsOn(buildDoclingWorker)
    inputs.dir(doclingWorkerResources)
    doLast {
        copy {
            from(doclingWorkerResources)
            into(layout.buildDirectory.dir("dist/wasmJs/productionExecutable"))
        }
    }
}
