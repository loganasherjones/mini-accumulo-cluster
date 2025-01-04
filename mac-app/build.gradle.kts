import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id("java")
    application
    id("com.bmuschko.docker-remote-api")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

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
}

tasks.test {
    useJUnitPlatform()
}

val buildImageTask by tasks.creating(DockerBuildImage::class) {
    dependsOn("distTar")
    buildArgs = mapOf("PROJECT_VERSION" to  project.version.toString())
    inputDir = file(project.projectDir)
    images.add("foo:latest")
}