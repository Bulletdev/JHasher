plugins {
    java
    application
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.bulletdev"
version = "2.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.bulletdev.jhasher.cli.JHasherCli"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.6")
    annotationProcessor("info.picocli:picocli-codegen:4.7.6")
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("com.nimbusds:nimbus-jose-jwt:10.3")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.fusesource.jansi:jansi:2.4.1")
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.fifesoft:rsyntaxtextarea:3.5.3")
    implementation("com.googlecode.lanterna:lanterna:3.1.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Aproject=com.bulletdev/jhasher"))
}

tasks.shadowJar {
    archiveClassifier = "all"
    mergeServiceFiles()
    manifest {
        attributes("Main-Class" to "com.bulletdev.jhasher.cli.JHasherCli")
    }
}

// jpackage - native installers (.deb, .rpm, .msi, .dmg)
// Usage: ./gradlew jpackageDeb   (Linux .deb)
//        ./gradlew jpackageRpm   (Linux .rpm)
//        ./gradlew jpackageMsi   (Windows)
//        ./gradlew jpackageDmg   (macOS)

fun jpackageTask(name: String, type: String, extraArgs: List<String> = emptyList()) =
    tasks.register<Exec>(name) {
        dependsOn(tasks.shadowJar)
        group = "distribution"
        description = "Build .$type installer via jpackage"
        val jarName = "jhasher-${project.version}-all.jar"
        val args = mutableListOf(
            "jpackage",
            "--type", type,
            "--name", "JHasher",
            "--app-version", project.version.toString().removeSuffix("-SNAPSHOT"),
            "--input", "build/libs",
            "--main-jar", jarName,
            "--main-class", "com.bulletdev.jhasher.cli.JHasherCli",
            "--dest", "build/dist",
            "--description", "Hash & Crypto Swiss Army Knife",
        )
        args.addAll(extraArgs)
        commandLine(args)
    }

jpackageTask("jpackageDeb", "deb", listOf(
    "--linux-deb-maintainer", "noreply@bulletdev.com",
    "--linux-app-category", "utils",
    "--linux-shortcut",
))

jpackageTask("jpackageRpm", "rpm", listOf(
    "--linux-app-category", "utils",
    "--linux-shortcut",
))

jpackageTask("jpackageMsi", "msi", listOf(
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser",
))

jpackageTask("jpackageDmg", "dmg")
