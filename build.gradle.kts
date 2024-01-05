/*
 * Copyright (c) 2019 - 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.io.FileInputStream
import java.util.Properties

val customCSSFile = projectDir.toString() + "/dokka/fr-backstage-styles.css"
val customLogoFile = projectDir.toString() + "/dokka/logo-icon.svg"
val customTemplatesFolder = file(projectDir.toString() + "/dokka/templates")

buildscript {

        val kotlin_version = "1.8.0"

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")
        classpath("com.adarshr:gradle-test-logger-plugin:2.0.0")
        classpath("com.google.gms:google-services:4.3.15")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
    id("org.sonatype.gradle.plugins.scan") version "2.4.0"
    id("org.jetbrains.dokka") version "1.9.10"
}

// Configure all single-project Dokka tasks at the same time,
// such as dokkaHtml, dokkaJavadoc and dokkaGfm.
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED,
                Visibility.PRIVATE,
                Visibility.INTERNAL,
                Visibility.PACKAGE
            )
        )
        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

allprojects {
    configurations.all {

        resolutionStrategy {
            // Due to vulnerability [CVE-2022-40152] from dokka project.
            force("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.5")
            force("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.5")
            force("com.fasterxml.jackson.core:jackson-databind:2.13.5")
            // Junit test project
            force("junit:junit:4.13.2")
            //Due to Vulnerability [CVE-2022-2390]: CWE-471 The product does not properly
            // protect an assumed-immutable element from being modified by an attacker.
            // on version < 18.0.1, this library is depended by most of the google libraries.
            // and needs to be reviewed on upgrades
            force("com.google.android.gms:play-services-basement:18.1.0")
            //Due to Vulnerability [CVE-2023-3635] CWE-681: Incorrect Conversion between Numeric Types
            //on version < 3.4.0, this library is depended by okhttp, when okhttp upgrade, this needs
            //to be reviewed
            force("com.squareup.okio:okio:3.4.0")
            //Due to this https://github.com/powermock/powermock/issues/1125, we have to keep using an
            //older version of mockito until mockito release a fix
            force("org.mockito:mockito-core:3.12.4")
            // this is for the mockwebserver
            force("org.bouncycastle:bcprov-jdk15on:1.68")
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

}

subprojects {

    apply(plugin = "org.jetbrains.dokka")

//    // configure only the HTML task
//    tasks.dokkaHtmlPartial {
//        outputDirectory.set(buildDir.resolve("docs/partial"))
//    }

    tasks.dokkaJavadoc {
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)

        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    Visibility.PUBLIC,
                    Visibility.PROTECTED,
                    Visibility.PRIVATE,
                    Visibility.INTERNAL,
                    Visibility.PACKAGE
                )
            )
            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true)
            }
        }

        doLast {
            exec {
                workingDir(project.rootDir)
                commandLine = "git checkout .".split(" ")
            }
        }
    }

    tasks.dokkaHtml {
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/html/${project.name}-dokka"))

        dokkaSourceSets.configureEach {
            documentedVisibilities.set(
                setOf(
                    Visibility.PUBLIC,
                    Visibility.PROTECTED,
                    Visibility.PRIVATE,
                    Visibility.INTERNAL,
                    Visibility.PACKAGE
                )
            )
            perPackageOption {
                matchingRegex.set(".*internal.*")
                suppress.set(true)
            }
        }

        doLast {
        exec {
            workingDir(project.rootDir)
            commandLine = "git checkout .".split(" ")
        }
     }
    }

    tasks.withType<DokkaTaskPartial>().configureEach {
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)
    }


    //Powermock compatibility with jdk 17
//    tasks.withType(Test).configureEach{
//        jvmArgs = jvmArgs + ['--add-opens=java.base/java.lang=ALL-UNNAMED']
//        jvmArgs = jvmArgs + ['--add-opens=java.base/java.security=ALL-UNNAMED']
//        jvmArgs = jvmArgs + ['--add-opens=java.base/java.security.cert=ALL-UNNAMED']
//    }

}

afterEvaluate {

    tasks.dokkaHtmlMultiModule {
        moduleName.set("ForgeRock SDK for Android")
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/api-reference/html"))
        val map = mutableMapOf<String, String>()
        map["org.jetbrains.dokka.base.DokkaBase"] = """{
                    "customStyleSheets": ["$customCSSFile"],
                    "templatesDir": "$customTemplatesFolder"
                }"""
        pluginsMapConfiguration.set(map)
    }


    tasks.dokkaJavadocCollector {
        moduleName.set("ForgeRock SDK for Android Javadoc")
        moduleVersion.set(project.property("VERSION") as? String)
        outputDirectory.set(file("build/api-reference/javadoc"))
    }

}


ossIndexAudit {
    username = System.getProperty("username")
    password = System.getProperty("password")
    excludeVulnerabilityIds = setOf("CVE-2020-15250")
}


//task.register(name = "type", type = Delete::class) {
//    delete(rootProject.buildDir)
//}

//task clean(type: Delete) {
//    delete rootProject.buildDir
//}

project.ext.set("versionName", "")
project.ext.set("versionCode", "")

ext["signing.keyId"] = ""
ext["signing.password"] = ""
ext["signing.secretKeyRingFile"] = ""
project.ext["ossrhUsername"] = ""
project.ext["ossrhPassword"] = ""

//val secretPropsFile = project.rootProject.file("local.properties")
//if (secretPropsFile.exists()) {
//    val p = Properties()
//    p.load(FileInputStream(secretPropsFile))
//    p.forEach { name, value ->
//        ext[name as? String] = value
//    }
//}
