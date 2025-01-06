import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("java")
    application
    id("com.bmuschko.docker-remote-api")
    id("com.avast.gradle.docker-compose")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

application {
    mainClass = "com.loganasherjones.mac.Main"
}

dependencies {
    implementation(project(":library"))
    implementation("org.apache.accumulo:accumulo-shell:${project.property("accumuloVersion")}")
    implementation("ch.qos.reload4j:reload4j:1.2.22")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":test-client"))
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dockerCompose {
    isRequiredBy(tasks.test)
    environment.put("PROJECT_VERSION", project.version.toString())
}

tasks.test {
    environment["PROJECT_VERSION"] = project.version.toString()
    useJUnitPlatform()
    dependsOn(startDefaultContainer)
    finalizedBy(stopDefaultContainer)
}

val buildImageTask by tasks.creating(DockerBuildImage::class) {
    val projectVersion = project.version.toString()
    dependsOn("distTar")
    dependsOn("compileTestJava")
    dependsOn("processTestResources")
    buildArgs = mapOf("PROJECT_VERSION" to  projectVersion)
    inputDir = file(project.projectDir)
    images.add("loganasherjones/mini-accumulo-cluster:${projectVersion}")
}


val createDefaultMacContainer by tasks.creating(DockerCreateContainer::class) {
    dependsOn(buildImageTask)
    dependsOn(":test-iterator:build")
    targetImageId(buildImageTask.imageId)
    exposePorts("tcp", listOf(21811))
    hostConfig.portBindings.set(listOf("21811:21811"))
    hostConfig.autoRemove.set(true)
    hostConfig.binds.set(mapOf("${project.layout.buildDirectory.get()}/iterators" to "/app/lib/ext"))
}

val startDefaultContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createDefaultMacContainer)
    targetContainerId(createDefaultMacContainer.containerId)
}

val stopDefaultContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createDefaultMacContainer.containerId)
}
