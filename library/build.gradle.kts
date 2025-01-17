import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("java")
    id("ca.cutterslade.analyze")
    id("com.vanniktech.maven.publish")
}

group = "com.loganasherjones"
version = rootProject.version

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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

val isReleaseVersion = !rootProject.version.toString().endsWith("SNAPSHOT")

mavenPublishing {
    configure(JavaLibrary(
        javadocJar = JavadocJar.Javadoc(),
        sourcesJar = true
    ))
    coordinates(
        groupId = group as String,
        artifactId = "mini-accumulo-cluster",
        version = rootProject.version as String,
    )

    pom {
        name = "mini-accumulo-cluster"
        description = "A mini accumulo cluster for integration testing"
        url = "https://github.com/loganasherjones/mini-accumulo-cluster"

        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/license/MIT"
            }
        }

        developers {
            developer {
                id = "loganasherjones"
                name = "Logan Asher Jones"
                email = "loganasherjones@gmail.com"
                organizationUrl = "https://github.com/loganasherjones"
            }
        }

        scm {
            connection = "scm:git:git://github.com/loganasherjones/mini-accumulo-cluster.git"
            developerConnection = "scm:git:ssh://github.com/loganasherjones/mini-accumulo-cluster.git"
            url = "https://github.com/loganasherjones/mini-accumulo-cluster"
        }

        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    }
    if (isReleaseVersion) {
        signAllPublications()
    }
}
