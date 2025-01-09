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
}

dependencies {
    implementation("org.apache.accumulo:accumulo-core:${project.property("accumuloVersion")}")
    implementation("org.apache.zookeeper:zookeeper:3.7.2")
    implementation(platform("org.junit:junit-bom:5.10.0"))
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("ch.qos.reload4j:reload4j:1.2.22")
    implementation("org.slf4j:slf4j-reload4j:1.7.36")
}

tasks.test {
    useJUnitPlatform()
}