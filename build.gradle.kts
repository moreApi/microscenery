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

dependencies {
    implementation("graphics.scenery:scenery:be073c7")
    // TODO fix imports
//    implementation("net.imagej:ij:1.49k")
//    implementation("net.imglib2:imglib2")
//    implementation("net.imglib2:imglib2-ij")
    // necessary for logging to work correctly
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

    implementation(files("C:/Program Files/Micro-Manager-2.0gamma/plugins/Micro-Manager/MMCoreJ.jar"))

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}