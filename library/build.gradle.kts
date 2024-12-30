plugins {
    id("java")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.accumulo:accumulo-minicluster:${project.property("accumuloVersion")}")
    implementation("org.apache.zookeeper:zookeeper:3.7.2")
    implementation("io.dropwizard.metrics:metrics-core:4.2.29")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
//    testImplementation("ch.qos.logback:logback-classic:1.2.13")
    testImplementation("ch.qos.reload4j:reload4j:1.2.22")
//    testImplementation("org.slf4j:jcl-over-slf4j:1.7.36")
//    testImplementation("org.slf4j:log4j-over-slf4j:1.7.36")
//    testImplementation("org.slf4j:reload4j-over-slf4j:1.7.36")
}

tasks.test {
    useJUnitPlatform()
}