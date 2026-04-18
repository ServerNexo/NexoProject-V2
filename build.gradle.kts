plugins {
    java
    // 🌟 FIX: El nuevo nombre oficial y la ultimísima versión del empaquetador
    id("com.gradleup.shadow") version "8.3.0" apply false
}

allprojects {
    group = "me.nexo"
    version = "1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library") // 🌟 FIX: Cambiado a java-library para poder compartir dependencias
    apply(plugin = "com.gradleup.shadow")

    // Todo lo de java y toolchain.languageVersion.set... se queda exactamente igual

    java {
        // Java 25 nativo absoluto
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.nexomc.com//releases")
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.triumphteam.dev/releases/")
        maven("https://jitpack.io")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}