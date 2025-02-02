plugins {
    id("java")
    id("com.bmuschko.docker-remote-api") version "9.4.0" apply false
    id("com.avast.gradle.docker-compose") version "0.17.12" apply false
    id("ru.vyarus.mkdocs") version "4.0.1"
    id("ca.cutterslade.analyze") version "1.10.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

repositories {
    mavenCentral()
}

val accumuloVersion = project.property("accumuloVersion").toString()
version = "${accumuloVersion}-2-SNAPSHOT"

mkdocs {
    sourcesDir = "."
    extras = mapOf("project_version" to version.toString())
    publish.repoUri = "git@github.com:loganasherjones/mini-accumulo-cluster"
}

subprojects {
    configurations.all {
        resolutionStrategy {
            force("org.slf4j:jcl-over-slf4j:2.0.16")
            force("org.apache.logging.log4j:log4j-to-slf4j:2.24.3")
        }
        exclude("org.apache.logging.log4j", "log4j-1.2-api")
    }
}
