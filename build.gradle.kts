plugins {
    java
//    signing
//    `maven-publish`
}

group = "net.sf.robocode"
description = "Codesize"
version = "1.3.0"


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