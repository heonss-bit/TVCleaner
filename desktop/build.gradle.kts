import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Общий код берём прямо из Android-модуля (модели, логика чистки), чтобы не дублировать.
val syncShared = tasks.register<Sync>("syncShared") {
    from("../app/src/main/java/app/tvcleaner") {
        include("data/Models.kt", "adb/TvOps.kt")
    }
    into(layout.buildDirectory.dir("generated/shared/app/tvcleaner"))
}

sourceSets.getByName("main").java.srcDir(layout.buildDirectory.dir("generated/shared"))
// Каталог мусора лежит в assets Android-модуля — на ПК читаем тот же файл
sourceSets.getByName("main").resources.srcDir("../app/src/main/assets")

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    dependsOn(syncShared)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("dev.mobile:dadb:1.2.10")
}

compose.desktop {
    application {
        mainClass = "app.tvcleaner.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "TVCleaner"
            packageVersion = "1.0.0"
            description = "Android TV cleaner"
            vendor = "heonss-bit"
            modules("java.instrument", "java.management", "java.naming", "java.sql", "jdk.unsupported")

            windows {
                menuGroup = "TV Cleaner"
                shortcut = true
                dirChooser = true
                perUserInstall = true
                if (!project.hasProperty("noIcon")) iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
