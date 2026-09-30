plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.paperweight)
    alias(libs.plugins.run.paper)
}

group = "com.catadmirer"

val javaVersion = (project.property("javaVersion") as String).toInt()
val minecraftVersion: String by project

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    compileOnly(libs.placeholderapi)
    compileOnly(libs.worldguard)
    compileOnly(libs.packetevents)
    compileOnly(libs.betterteams)
    compileOnly(libs.guava)
    compileOnly(libs.gson)

    paperweight.paperDevBundle("${minecraftVersion}+")
}

tasks.runServer {
    // Configure the Minecraft version for our task.
    // This is the only required configuration besides applying the plugin.
    // Your plugin's jar (or shadowJar if present) will be used automatically.
    minecraftVersion(minecraftVersion)
    jvmArgs("-Dlog4j.configurationFile=log4j2.xml")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
    sourceCompatibility = JavaVersion.toVersion(javaVersion)
    targetCompatibility = JavaVersion.toVersion(javaVersion)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

tasks.processResources {
    val props = mapOf("version" to version,
        "mcVersion" to minecraftVersion)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.register("resetAndRun") {
    description = "Resets the server files and reruns it"
    delete("run/plugins/$rootProject.name")
    finalizedBy("runServer")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "infuse"
        }
    }
    repositories {
        maven {
            name = "turbo-maven"
            url = uri("https://maven.turbojax.org/releases/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}