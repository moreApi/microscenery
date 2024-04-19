
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
    // - jans-branch
    // - fix/16bit-histogram
    implementation("com.github.scenerygraphics:scenery:4b0ea44b36192317e01bb7f833e8fc2f819d3a84")
    // necessary for logging to work correctly
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")

    implementation(project(":core"))
    implementation(project(":zenSysConCon"))
    implementation(files("../core/manualLib/MMCoreJ.jar"))

    implementation("org.yaml:snakeyaml") {
        version { strictly("1.33") }
    }

    testImplementation("net.imagej:ij:1.53k")
    testImplementation("net.imagej:imagej-ops:0.45.5")
    testImplementation("net.imglib2:imglib2")
    testImplementation("net.imglib2:imglib2-ij:2.0.0-beta-30")
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

tasks{
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

    withType<JavaCompile>().all {
        targetCompatibility = "21"
        sourceCompatibility = "21"
    }
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
