import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import java.io.BufferedReader
import java.nio.file.Paths

val javaVersion = 17
val minecraftVersion = "1.20.4"

fun runCommand(command: Array<String>) = Runtime
    .getRuntime()
    .exec(command)
    .let { process ->
        process.waitFor()
        val output = process.inputStream.use {
            it.bufferedReader().use(BufferedReader::readText)
        }
        process.destroy()
        output.trim()
    }

// example version number: MPP-0.1.302-1.19.4-SNAPSHOT
// when changing version be sure to only use the chars mentioned in the comments
// otherwise you have to change the regex in the workflows
fun getPluginVersion(): String {
    val baseVersion = "0.1" // only 0-9. not empty
    val versionSuffix = "SNAPSHOT" // only A-Z not empty
    val commitCount = runCommand(arrayOf("git", "rev-list", "--count", "HEAD")).trim() // only 0-9 not empty
    return "$baseVersion.$commitCount-$minecraftVersion-$versionSuffix"
}

group = "de.danielmaile.mpp"
version = getPluginVersion()

plugins {
    `java-library`
    `embedded-kotlin`
    id("io.papermc.paperweight.userdev") version "1.5.3"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.3.1"
    id("de.undercouch.download") version "5.4.0"
}

bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    main = "de.danielmaile.mpp.MPP"
    apiVersion = "1.19"
    authors = listOf("Daniel Maile", "ChechuDEV", "Others")
    libraries = libs.bundles.plugin.get().map { "${it.group}:${it.name}:${it.version}" }
    commands {
        register("mpp") {
            permission = "mpp.mpp"
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
}

repositories {
    mavenCentral()
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.mattstudios.me/artifactory/public/")
}

dependencies {
    paperweight.paperDevBundle("$minecraftVersion-R0.1-SNAPSHOT")
    implementation(libs.bundles.implementation) {
        exclude(group = "net.kyori", module = "adventure-api")
        exclude(group = "net.kyori", module = "adventure-text-serializer-legacy")
        exclude(group = "net.kyori", module = "adventure-text-serializer-gson")
    }

    compileOnly(libs.bundles.compile)
    testImplementation(libs.bundles.test)
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(javaVersion)
    }
    shadowJar {
        relocate("dev.triumphteam.gui", "de.danielmaile.libs.gui")
        relocate("org.bstats", "de.danielmaile.libs.bStats")
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    build {
        finalizedBy("buildResourcePack")
    }

    runServer {
        jvmArgs("-Dcom.mojang.eula.agree=true")
        pluginJars(File(buildDir, "ProtocolLib.jar"))
        minecraftVersion(minecraftVersion)
    }

    // region group often used tasks for convenience
    clean {
        group = "mpp"
    }
    build {
        group = "mpp"
    }
    ktlintCheck {
        group = "mpp"
    }
    runServer {
        group = "mpp"
    }

    test {
        useJUnitPlatform()
    }
    // endregion
}

tasks.register<JavaExec>("buildResourcePack") {
    mainClass.set("de.danielmaile.resourcepack.ResourcePackBuilderKt")
    classpath(sourceSets["main"].runtimeClasspath, configurations.compileClasspath)
    group = "mpp"
    args = listOf(getPluginVersion(), project.projectDir.absolutePath)
}

tasks.register<RunResourcePackServer>("runResourcePackServer") {
    dependsOn("buildResourcePack")
    group = "mpp"
    port = 8080
    resourcePackZip = File(
        projectDir.toPath().resolve(Paths.get("build", "resourcepack")).toFile(),
        "MPP-${getPluginVersion()}.zip"
    )
}
