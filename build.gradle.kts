import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.7.10"
    id("com.google.protobuf") version "0.8.19"
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
    implementation("graphics.scenery:scenery:8189e8")
    // necessary for logging to work correctly
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")


//    implementation("org.bytedeco:ffmpeg:4.3.2-1.5.5", ffmpegNatives)
//    implementation("org.bytedeco.javacpp-presets:ffmpeg:4.1-1.4.4")
//    implementation("org.bytedeco.javacpp-presets:ffmpeg-platform:4.1-1.4.4")
    implementation ("com.github.stuhlmeier:kotlin-events:v2.0")
    implementation ("com.google.protobuf:protobuf-java:3.21.5")

    implementation(files("manualLib/MMCoreJ.jar"))

    testImplementation("net.imagej:ij:1.53k")
    testImplementation("net.imagej:imagej-ops:0.45.5")
    testImplementation("net.imglib2:imglib2")
    testImplementation("net.imglib2:imglib2-ij:2.0.0-beta-30")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation(kotlin("test"))
    //testImplementation(kotlin("test-junit"))
}

java.sourceSets["main"].java {
    srcDir("build/generated/source/proto/main/java")
}

protobuf {
    this.protobuf.protoc {
        this.path = "protoc/bin/protoc.exe"
    }
}

tasks.test {
    jvmArgs = listOf("-Xmx28G")
    //useJUnit()
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}


tasks{
    // This registers gradle tasks for all example scenes
    sourceSets.test.get().allSource.files
        .map { it.path.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt") }
        .filter { it.contains("microscenery.example.") && !it.contains("resources") }
        .forEach { className ->
            val exampleName = className.substringAfterLast(".")
            val exampleType = className.substringBeforeLast(".").substringAfterLast(".")

            register<JavaExec>(name = exampleName) {
                classpath = sourceSets.test.get().runtimeClasspath
                main = className
                group = "examples.$exampleType"

                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

                val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
                allJvmArgs = if (additionalArgs != null) {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
                } else {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
                }
            }
        }

    register("copyRuntimeLibs", Copy::class) {
        into("microsceneryDependencies")
        from(configurations.runtimeClasspath)
    }
}
