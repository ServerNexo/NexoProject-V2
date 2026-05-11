plugins {
    java
}

group = "me.nexo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // Jitpack ahora servirá para NoteBlockAPI y para Lamp 3.2.1
}

dependencies {
    // 🌟 API Base (Folia/Paper 1.21.5)
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // 🌟 DEPENDENCIAS INTERNAS (Tus otros módulos)
    compileOnly(project(":NexoCore"))
    compileOnly(project(":NexoIslas"))
    compileOnly(project(":NexoMinions"))

    // 🌟 LIBRERÍAS EXTERNAS Y APIS
    compileOnly("com.github.koca2000:NoteBlockAPI:1.6.2") // Para el Boombox Premium
    compileOnly("com.nexomc:nexo:1.20.1") // O la versión local que uses de NexoItems

    // 🌟 SILENCIADOR DE WARNINGS (Para el compilador de Nexo/Kotlin)
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // 🌟 INYECCIÓN DE DEPENDENCIAS Y COMANDOS
    compileOnly("com.google.inject:guice:7.0.0")

    // 🌟 FIX: Igualamos exactamente a la versión de Lamp de tu NexoCore
    compileOnly("com.github.revxrsal.Lamp:common:3.2.1")
    compileOnly("com.github.revxrsal.Lamp:bukkit:3.2.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}