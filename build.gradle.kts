plugins {
    id("java")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    configurations.all {
        // I'm using logback for everything. Many of the accumulo dependencies
        // seem to pull in random logging implementations. We will force
        // everyone to use slf4j, and then, when necessary add logback as the
        // implementation.
        resolutionStrategy {
            force("org.slf4j:jcl-over-slf4j:1.7.36")
            force("org.slf4j:log4j-over-slf4j:1.7.36")
            force("org.slf4j:reload4j-over-slf4j:1.7.36")
            force("org.slf4j:slf4j-api:1.7.36")
        }
        exclude("org.slf4j", "slf4j-reload4j")
        exclude("org.slf4j", "slf4j-log4j")
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("log4j", "log4j")
        exclude("ch.qos.reload4j", "reload4j")
    }
}
