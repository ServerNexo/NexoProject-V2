plugins {
    java
    // 🌟 Motor Shadow actualizado
    id("com.gradleup.shadow") version "9.4.1" apply false
}

allprojects {
    group = "me.aeroxis"
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

        // PAPER API
        compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

        // 🌟 FIX CRÍTICO: Todo a compileOnly para que no se clonen en los Addons
        compileOnly("com.google.inject:guice:7.0.0")
        compileOnly("com.zaxxer:HikariCP:5.1.0")
        compileOnly("org.postgresql:postgresql:42.7.11")
        compileOnly("com.github.ben-manes.caffeine:caffeine:3.1.8")
    }

    // ==========================================
    // 🛡️ ESCUDO DE SEGURIDAD GLOBAL (AEROXIS ECOSYSTEM)
    // ==========================================
    configurations.all {
        // Obliga a actualizar incluso las librerías transitivas en compileOnly
        resolutionStrategy {
            force("org.postgresql:postgresql:42.7.11")
            force("org.codehaus.plexus:plexus-utils:4.0.3")
            force("org.apache.commons:commons-lang3:3.18.0")
            force("com.google.guava:guava:32.0.1-jre")
            force("org.yaml:snakeyaml:2.2")
        }

        // Excluye las versiones viejas de Maven que arrastra Paper-API
        exclude(group = "org.apache.maven")
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