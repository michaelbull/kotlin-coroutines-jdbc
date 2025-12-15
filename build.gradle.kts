import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.benmanes.gradle.versions.updates.gradle.GradleReleaseChannel
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.versions)
    alias(libs.plugins.maven.publish)
}

tasks.withType<DependencyUpdatesTask> {
    gradleReleaseChannel = GradleReleaseChannel.CURRENT.id

    rejectVersionIf {
        listOf("alpha", "beta", "rc", "cr", "m", "eap", "pr", "dev").any {
            candidate.version.contains(it, ignoreCase = true)
        }
    }
}

tasks.withType<Jar> {
    from(rootDir.resolve("LICENSE")) {
        into("META-INF")
    }
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }
}

dependencies {
    implementation(libs.kotlin.inline.logger)
    implementation(libs.kotlin.coroutines.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.coroutines.test)
    testImplementation(libs.mockk)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Empty(),
            sourcesJar = true,
        )
    )

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
            url.set("https://github.com/michaelbull/kotlin-coroutines-jdbc/actions")
        }
    }
}
