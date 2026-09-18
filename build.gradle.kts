plugins {
    id("java-library")
}

group = "net.minecraft"
version = "a0.1.0"

repositories {
    mavenCentral()
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val lwjglNatives = when {
    osName.contains("win") -> if (osArch.contains("aarch64")) "natives-windows-arm64" else "natives-windows"
    osName.contains("linux") -> if (osArch.contains("aarch64")) "natives-linux-arm64" else "natives-linux"
    osName.contains("mac") -> if (osArch.contains("aarch64")) "natives-macos-arm64" else "natives-macos"
    else -> throw Error("Unsupported Platform: $osName")
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

    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-glfw", classifier = lwjglNatives)
    runtimeOnly("org.lwjgl", "lwjgl-opengl", classifier = lwjglNatives)
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
    archiveBaseName.set("mc-server")
    archiveVersion.set(version.toString())
    destinationDirectory.set(project.layout.projectDirectory.dir("build/dist").asFile)
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
    archiveBaseName.set("mc-client")
    archiveVersion.set(version.toString())
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