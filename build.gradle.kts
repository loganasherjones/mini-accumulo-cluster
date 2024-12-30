plugins {
    id("java")
    id("com.bmuschko.docker-remote-api") version "9.4.0" apply false
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    configurations.all {
        // Accumulo uses reload4j for its logging. I'd like to use logback, but
        // when I try, everything fails because log4j/helpers/FileWatchdog doesn't
        // exist on the classpath. The following ensures that everything works with
        // reload4j as runner.
        resolutionStrategy {
            force("org.slf4j:jcl-over-slf4j:1.7.36")
//            force("org.slf4j:log4j-over-slf4j:1.7.36")
            force("org.slf4j:slf4j-api:1.7.36")
        }
//        exclude("org.slf4j", "slf4j-reload4j")
        exclude("org.slf4j", "slf4j-log4j")
        exclude("org.slf4j", "slf4j-log4j12")
        exclude("log4j", "log4j")
//        exclude("ch.qos.reload4j", "reload4j")
    }
}
