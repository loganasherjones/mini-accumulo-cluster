plugins {
    id("java")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.accumulo:accumulo-core:${project.property("accumuloVersion")}")
}

val copyJars by tasks.register("copyJars", Copy::class) {
    from(tasks.jar.get().outputs) // Copies the project's JAR
    into("${project(":mac-app").layout.buildDirectory.get()}/iterators")
}

tasks.build {
    finalizedBy(copyJars) // Ensures copyJars runs after the build task
}
