import com.google.protobuf.gradle.protoc

plugins {
    kotlin("jvm") version embeddedKotlinVersion
    id("com.google.protobuf") version "0.8.19"
}

group = "me.jancasus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://jitpack.io")
}


dependencies {
    implementation(kotlin("reflect"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.joml:joml:1.10.5")
    if(System.getProperty("os.name").lowercase().contains("mac")) {
        implementation ("com.google.protobuf:protobuf-java:4.29.1") //current macOS homebrew version
    } else {
        implementation ("com.google.protobuf:protobuf-java:3.25.3") // I think this one was still compatible with micromanager
    }
    implementation("com.google.protobuf:protobuf-java-util:3.25.3")
    implementation("org.zeromq:jeromq:0.5.2")
    implementation ("com.github.stuhlmeier:kotlin-events:v2.0")
    // this is the version micromanager uses
    implementation("com.miglayout:miglayout:3.7.4")

    implementation("org.jmdns:jmdns:3.5.9")

    val lwjglVersion = "3.3.1"
    val lwjglNatives = listOf("natives-linux", "natives-windows", "natives-macos", "natives-macos-arm64")
    implementation("org.lwjgl", "lwjgl")
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    lwjglNatives.forEach { runtimeOnly("org.lwjgl", "lwjgl", classifier = it) }

    implementation(files("manualLib/MMCoreJ.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.lwjgl:lwjgl-jawt:3.3.1")
    testImplementation(kotlin("test"))
}


tasks.test {
    jvmArgs = listOf("-Xmx28G")
    useJUnitPlatform()
}

java.sourceSets["main"].java {
    srcDir("build/generated/source/proto/main/java")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

sourceSets["main"].proto {
    srcDir("microscenery-protocol/proto")
}
protobuf {
    var protocPath = providers.gradleProperty("protoc.path").orNull
    if(protocPath == null) {
        logger.warn("protoc path property (protoc.path) not found, falling back to default.")
        protocPath = projectDir.resolve("protoc/bin/protoc.exe").absolutePath
    }

    logger.info("Using protoc from $protocPath")
    this.protobuf.protoc {
        this.path = protocPath
    }
}

tasks{
    register("copyRuntimeLibs", Copy::class) {
        into("microsceneryDependencies")
        from(configurations.runtimeClasspath)
    }
    jar{
        this.archiveBaseName.set("microscenery-core")
    }
}