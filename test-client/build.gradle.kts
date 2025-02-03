plugins {
    id("java")
}

group = "com.loganasherjones"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

val accumuloVersion = project.property("accumuloVersion").toString()
val zookeeperVersion = project.property("zookeeperVersion").toString()

dependencies {
    implementation("org.apache.accumulo:accumulo-core:$accumuloVersion")
    implementation("org.apache.zookeeper:zookeeper:$zookeeperVersion")
    implementation(platform("org.junit:junit-bom:5.10.0"))
    implementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}