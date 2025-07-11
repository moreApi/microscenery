
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version embeddedKotlinVersion
}

group = "me.jancasus"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.scijava.org/content/groups/public")
}

dependencies {
    // This should point to the most recent commit of scenery:jans-dirtier-branch
    // At the moment scenery:jans-dirtier-branch should be a merge of:
    // - jans-branch
    // - fix/16bit-histogram
    // - fix/windows-mouse-click-scroll
    // - split-histogram-checkbox
    // - improveDisplayRangeLimitsNaming
    // - fix/attachment-order-in-dssdo-shaders
    implementation("com.github.scenerygraphics:scenery:d2c7e737ad63b2ba7cb05c42c9c0e09acb6e63ca")
    // necessary for logging to work correctly
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.25.0")

    implementation(project(":core"))
    implementation(project(":zenSysConCon"))
    implementation(files("../core/manualLib/MMCoreJ.jar"))

    implementation("org.yaml:snakeyaml") {
        version { strictly("1.33") }
    }
    val scijavaParentPomVersion = project.properties["scijavaParentPOMVersion"]
    implementation(platform("org.scijava:pom-scijava:$scijavaParentPomVersion"))

    testImplementation("net.imagej:imagej")
    testImplementation("net.imagej:ij")
    testImplementation("net.imglib2:imglib2-ij")

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
        .filter { it.contains("microscenery.scenes.") && !it.contains("resources") }
        .forEach { className ->
            val exampleName = className.substringAfterLast(".")
            val exampleType = className.substringBeforeLast(".").substringAfterLast(".")

            register<JavaExec>(name = exampleName) {
                classpath = sourceSets.test.get().runtimeClasspath
                mainClass.set(className)
                group = "examples.$exampleType"
                workingDir = workingDir.parentFile
                jvmArguments.add("-Dscenery.Renderer.Device=NVIDIA")

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
