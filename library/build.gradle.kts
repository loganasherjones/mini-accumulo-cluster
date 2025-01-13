plugins {
    id("java")
    id("maven-publish")
    id("ca.cutterslade.analyze")
}

group = "com.loganasherjones"
version = rootProject.version

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

val accumuloVersion = project.property("accumuloVersion").toString()

dependencies {
    implementation("org.apache.accumulo:accumulo-core:${accumuloVersion}")
    implementation("org.apache.accumulo:accumulo-gc:${accumuloVersion}")
    implementation("org.apache.accumulo:accumulo-master:${accumuloVersion}")
    implementation("org.apache.accumulo:accumulo-server-base:${accumuloVersion}")
    implementation("org.apache.accumulo:accumulo-shell:${accumuloVersion}")
    implementation("org.apache.accumulo:accumulo-tserver:${accumuloVersion}")
    implementation("org.apache.commons:commons-vfs2:2.3")
    implementation("org.apache.zookeeper:zookeeper:3.7.2")
    implementation("org.slf4j:slf4j-api:1.7.36")
    runtimeOnly("io.dropwizard.metrics:metrics-core:4.2.29")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation(project(":test-client"))
    testRuntimeOnly(project(":test-iterator"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "mini-accumulo-cluster"
            from(components["java"])
        }
    }
}