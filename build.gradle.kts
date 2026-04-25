plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.william"
version = "0.1.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

// --- Local test server ---

val serverDir = layout.projectDirectory.dir("run")
val paperJar = serverDir.file("paper.jar")
val paperBuild = 194
val paperVersion = "1.21.4"

tasks.register("downloadPaper") {
    description = "Downloads the Paper server jar if missing"
    val jar = paperJar.asFile
    outputs.file(jar)
    onlyIf { !jar.exists() }
    doLast {
        jar.parentFile.mkdirs()
        val url = "https://api.papermc.io/v2/projects/paper/versions/$paperVersion/builds/$paperBuild/downloads/paper-$paperVersion-$paperBuild.jar"
        uri(url).toURL().openStream().use { it.copyTo(jar.outputStream()) }
        serverDir.file("eula.txt").asFile.writeText("eula=true\n")
    }
}

tasks.register<Copy>("copyPlugin") {
    description = "Copies the plugin jar into the test server"
    dependsOn(tasks.build)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(serverDir.dir("plugins"))
}

tasks.register<JavaExec>("runServer") {
    description = "Builds the plugin and starts a local Paper server"
    dependsOn("downloadPaper", "copyPlugin")
    classpath(paperJar)
    workingDir(serverDir)
    jvmArgs("-Xmx1G", "-Xms512M")
    args("--nogui")
    standardInput = System.`in`
}
