plugins {
    id("java-library")
}

group = "net.minecraft"

val versionFile = file("resources/version.txt").takeIf { it.exists() }
    ?: file("src/main/resources/version.txt").takeIf { it.exists() }
val appVersion = versionFile?.readText()?.trim()?.take(20) ?: "unknown"
version = appVersion

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    val lwjglVersion = "3.3.4"
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-opengl")

    // compatibility
    val platforms = listOf(
        "natives-windows", "natives-windows-arm64",
        "natives-linux", "natives-linux-arm64",
        "natives-macos", "natives-macos-arm64"
    )

    for (platform in platforms) {
        runtimeOnly("org.lwjgl", "lwjgl", classifier = platform)
        runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = platform)
        runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = platform)
    }
}

tasks.register<JavaExec>("runClient") {
    mainClass.set("client.Minecraft")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = project.layout.projectDirectory.dir("run").asFile
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED")

    doFirst { workingDir.mkdirs() }
}

tasks.named<Wrapper>("wrapper") {
    distributionUrl = "gradle-9.0.0-bin.zip"
}

tasks.register<JavaExec>("runServer") {
    mainClass.set("server.Server")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = project.layout.projectDirectory.dir("run").asFile
    doFirst { workingDir.mkdirs() }
}

tasks.register<Jar>("buildServer") {
    group = "build"
    dependsOn("classes")

    archiveFileName.set("Finecraft Server $appVersion.jar")
    destinationDirectory.set(project.layout.projectDirectory.dir("build/dist").asFile)

    manifest {
        attributes("Main-Class" to "server.Server")
    }

    from(sourceSets["main"].output)
    from(
        configurations.runtimeClasspath.get()
            .filter { !it.name.contains("lwjgl") }
            .map { if (it.isDirectory) it else zipTree(it) }
    ) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.register<Jar>("buildClient") {
    group = "build"
    dependsOn("classes")

    archiveFileName.set("Finecraft Client $appVersion.jar")
    destinationDirectory.set(project.layout.projectDirectory.dir("build/dist").asFile)

    manifest {
        attributes("Main-Class" to "client.Minecraft")
    }

    from(sourceSets["main"].output)
    from(
        configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) }
    ) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}