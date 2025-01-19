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
version = "${accumuloVersion}-0"

mkdocs {
    sourcesDir = "."
    extras = mapOf("project_version" to version.toString())
    publish.repoUri = "git@github.com:loganasherjones/mini-accumulo-cluster"
}

subprojects {
    configurations.all {
        // Accumulo uses reload4j for its logging. I'd like to use logback, but
        // when I try, everything fails because log4j/helpers/FileWatchdog doesn't
        // exist on the classpath. The following ensures that everything works with
        // reload4j as runner.
        resolutionStrategy {
            force("org.slf4j:jcl-over-slf4j:1.7.36")
            force("org.slf4j:slf4j-api:1.7.36")
        }
        exclude("org.slf4j", "slf4j-log4j")
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("log4j", "log4j")
    }
}
