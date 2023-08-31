
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version embeddedKotlinVersion
}

group = "me.jancasus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://jitpack.io")
}

dependencies {
    // This should point to the most recent commit of scenery:jans-dirtier-branch
    // At the moment scenery:jans-dirtier-branch should be a merge of:
    // - branch-of-jan
    // - LineColorFix
    // TODO WIP WIP evrything is broken and temp
    implementation("graphics.scenery:scenery:ef46bb4")
    // necessary for logging to work correctly
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

    implementation(project(":core"))
    implementation(project(":zenSysConCon"))
    implementation(files("../core/manualLib/MMCoreJ.jar"))

//    implementation("org.bytedeco:ffmpeg:4.3.2-1.5.5", ffmpegNatives)
//    implementation("org.bytedeco.javacpp-presets:ffmpeg:4.1-1.4.4")
//    implementation("org.bytedeco.javacpp-presets:ffmpeg-platform:4.1-1.4.4")

    testImplementation("net.imagej:ij:1.53k")
    testImplementation("net.imagej:imagej-ops:0.45.5")
    testImplementation("net.imglib2:imglib2")
    testImplementation("net.imglib2:imglib2-ij:2.0.0-beta-30")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.mockito:mockito-core:4.8.0")
    testImplementation("org.mockito:mockito-inline:4.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation(kotlin("test"))
    //testImplementation(kotlin("test-junit"))
}


tasks.test {
    jvmArgs = listOf("-Xmx28G")
    //useJUnit()
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
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
                mainClass.set(className)
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
}
