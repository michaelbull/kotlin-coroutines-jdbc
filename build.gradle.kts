import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ossrhUsername: String? by project
val ossrhPassword: String? by project

val signingKeyId: String? by project // must be the last 8 digits of the key
val signingKey: String? by project
val signingPassword: String? by project

description = "A library for interacting with blocking JDBC drivers using Kotlin Coroutines."

plugins {
    `maven-publish`
    signing
    kotlin("jvm") version "1.6.10"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("org.jetbrains.dokka") version "1.6.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.michael-bull.kotlin-inline-logger:kotlin-inline-logger:1.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.1")
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        listOf("alpha", "beta", "rc", "cr", "m", "eap", "pr").any {
            candidate.version.contains(it, ignoreCase = true)
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.contracts.ExperimentalContracts")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val dokkaJavadoc by tasks.existing(DokkaTask::class)

val javadocJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles a jar archive containing the Javadoc API documentation."
    archiveClassifier.set("javadoc")
    from(dokkaJavadoc)
}

val sourcesJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Assembles a jar archive containing the main classes with sources."
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    repositories {
        maven {
            if (project.version.toString().endsWith("SNAPSHOT")) {
                setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            } else {
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            }

            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(javadocJar.get())
            artifact(sourcesJar.get())

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/michaelbull/kotlin-coroutines-jdbc")
                inceptionYear.set("2019")

                licenses {
                    license {
                        name.set("ISC License")
                        url.set("https://opensource.org/licenses/isc-license.txt")
                    }
                }

                developers {
                    developer {
                        name.set("Michael Bull")
                        url.set("https://www.michael-bull.com")
                    }
                }

                contributors {
                    contributor {
                        name.set("huntj88")
                        url.set("https://github.com/huntj88")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/michaelbull/kotlin-coroutines-jdbc")
                    developerConnection.set("scm:git:git@github.com:michaelbull/kotlin-coroutines-jdbc.git")
                    url.set("https://github.com/michaelbull/kotlin-coroutines-jdbc")
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/michaelbull/kotlin-coroutines-jdbc/issues")
                }

                ciManagement {
                    system.set("GitHub")
                    url.set("https://github.com/michaelbull/kotlin-coroutines-jdbc/actions?query=workflow%3Aci")
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}
