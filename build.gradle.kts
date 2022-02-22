import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10"
}

group = "me.jancasus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://jitpack.io")
}

val ffmpegNatives = arrayOf("windows-x86_64", "linux-x86_64", "macosx-x86_64")
fun DependencyHandlerScope.implementation(dep: String, natives: Array<String>) {
    add("implementation", dep)
    val split = dep.split(':')
    for (native in natives)
        org.gradle.kotlin.dsl.accessors.runtime.addExternalModuleDependencyTo(
            this@implementation, "runtimeOnly",
            split[0], split[1], split.getOrNull(2), null,
            native, null, null
        )
}

dependencies {
    implementation("graphics.scenery:scenery:be073c7")
    // TODO fix imports
//    implementation("net.imagej:ij:1.49k")
//    implementation("net.imglib2:imglib2")
//    implementation("net.imglib2:imglib2-ij")
    // necessary for logging to work correctly
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")


    implementation("org.bytedeco:ffmpeg:4.3.2-1.5.5", ffmpegNatives)
//    implementation("org.bytedeco.javacpp-presets:ffmpeg:4.1-1.4.4")
//    implementation("org.bytedeco.javacpp-presets:ffmpeg-platform:4.1-1.4.4")

    implementation(files("C:/Program Files/Micro-Manager-2.0gamma/plugins/Micro-Manager/MMCoreJ.jar"))

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}