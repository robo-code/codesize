plugins {
    java
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.0.0"
    idea
}

group = "net.sf.robocode"
description = "Codesize is a tool to calculate the byte size of Java bytecode"
version = "1.3.0"

val ossrhUsername: String by project
val ossrhPassword: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.bcel:bcel:6.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes["Main-Class"] = "codesize.Codesize"
            attributes["Implementation-Title"] = "Codesize"
            attributes["Implementation-Version"] = archiveVersion
            attributes["Implementation-Vendor"] = "robocode.dev"
            attributes["Package"] = project.group
        }
        archiveFileName.set("codesize.jar")
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://oss.sonatype.org/service/local/"))//staging/deploy/maven2/
            snapshotRepositoryUrl.set(uri("https://oss.sonatype.org/content/repositories/snapshots/"))
            stagingProfileId.set("c7f511545ccf8")
            username.set(ossrhUsername)
            password.set(ossrhPassword)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Codesize")
                description.set(project.description)
                url.set("https://robocode.sourceforge.io/")
                licenses {
                    license {
                        name.set("Eclipse Public License v1.0 (EPL)")
                        url.set("https://robocode.sourceforge.io/license/epl-v10.html")
                    }
                }
                developers {
                    developer {
                        name.set("Christian D. Schnell")
                    }
                    developer {
                        id.set("flemming-n-larsen")
                        name.set("Flemming N. Larsen")
                        email.set("flemming.n.larsen@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:robo-code/robocode.git")
                    developerConnection.set("scm:git:ssh:git@github.com:robo-code/robocode.git")
                    url.set("https://github.com/robo-code/robocode")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}


val initializeSonatypeStagingRepository by tasks.existing
subprojects {
    initializeSonatypeStagingRepository {
        shouldRunAfter(tasks.withType<Sign>())
    }
}