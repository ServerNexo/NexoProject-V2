plugins {
    java
    // 🌟 Motor Shadow actualizado
    id("com.gradleup.shadow") version "9.4.1" apply false
}

allprojects {
    group = "me.nexo"
    version = "1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.gradleup.shadow")

    java {
        // Java 21 Nativo Absoluto
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.nexomc.com/releases")
        maven("https://repo.opencollab.dev/main/")
        maven("https://repo.auxilor.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.triumphteam.dev/releases/")
        maven("https://jitpack.io")
        maven("https://mvn.lumine.io/repository/maven-public/")
        maven("https://maven.enginehub.org/repo/")
    }

    dependencies {
        // LOMBOK
        compileOnly("org.projectlombok:lombok:1.18.34")
        annotationProcessor("org.projectlombok:lombok:1.18.34")

        // 🌟 FIX CRÍTICO: Estas dos líneas apagarán los errores rojos en tu IDE
        compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
        implementation("com.google.inject:guice:7.0.0")

        implementation("com.zaxxer:HikariCP:5.1.0")
        implementation("org.postgresql:postgresql:42.7.2")
        implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    // 🌟 FIX: Inyección elegante y segura a la ruta de tu servidor local
    tasks.withType<org.gradle.api.tasks.bundling.Jar>().configureEach {
        if (name == "shadowJar") {
            // 🚀 Destino de los .jar directo a la vena del servidor
            destinationDirectory.set(file("C:/Users/faust/Desktop/NexoV2/plugins"))
            archiveClassifier.set("")
        }
    }
}