plugins {
    id("java")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.apache.accumulo:accumulo-minicluster:${project.property("accumuloVersion")}")
    implementation("org.apache.zookeeper:zookeeper:3.7.2")
    implementation("io.dropwizard.metrics:metrics-core:4.2.29")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(project(":test-client"))
    testImplementation(project(":test-iterator"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("ch.qos.reload4j:reload4j:1.2.22")
}

tasks.test {
    useJUnitPlatform()
}