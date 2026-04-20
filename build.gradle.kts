plugins {
    java
    // 🌟 FIX: Motor Shadow actualizado para soporte total con Gradle 9.2.0 y Java 21+
    id("com.gradleup.shadow") version "9.4.1" apply false
}

allprojects {
    group = "me.nexo"
    version = "1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library") // 🌟 FIX: Cambiado a java-library para poder compartir dependencias
    apply(plugin = "com.gradleup.shadow")

    java {
        // Java nativo absoluto
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://maven.enginehub.org/repo/")
    }

    // 🌟 FIX: Inyectamos Lombok globalmente para TODOS los módulos
    dependencies {
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // 🌟 FIX: Ruteo directo al Servidor de Pruebas
    tasks.named<org.gradle.api.tasks.bundling.Jar>("shadowJar") {
        // 🚀 MAGIA: Ahora los .jar volarán directamente a tu servidor local
        destinationDirectory.set(file("C:/Users/faust/Desktop/NexoV2/plugins"))

        // Removemos el sufijo "-all" para que el archivo quede con un nombre limpio
        archiveClassifier.set("")
    }
}