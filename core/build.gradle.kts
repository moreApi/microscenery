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

val lwjglVersion = "3.3.1"
val lwjglNatives = "natives-windows"

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.joml:joml:1.10.5")
    implementation ("com.google.protobuf:protobuf-java:3.21.7")
    implementation("com.google.protobuf:protobuf-java-util:3.21.7")
    implementation("org.zeromq:jeromq:0.5.2")
    implementation ("com.github.stuhlmeier:kotlin-events:v2.0")
    // this is the version micromanager uses
    implementation("com.miglayout:miglayout:3.7.4")



    implementation(files("manualLib/MMCoreJ.jar"))

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl", "lwjgl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
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