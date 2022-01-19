import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.projectronin.interop.gradle.base") version "1.0.0-SNAPSHOT"

    `kotlin-dsl`
    `maven-publish`
    jacoco
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
}

repositories {
    maven {
        name = "ronin"
        url = uri("https://maven.pkg.github.com/projectronin/package-repo")
        credentials {
            username = System.getenv("PACKAGE_USER")
            password = System.getenv("PACKAGE_TOKEN")
        }
    }
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("com.projectronin.interop.gradle.base:com.projectronin.interop.gradle.base.gradle.plugin:1.0.0-SNAPSHOT")

    implementation("io.spring.gradle:dependency-management-plugin:1.0.11.RELEASE")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
    implementation("org.jetbrains.kotlin:kotlin-allopen:1.5.31")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:10.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    // Allows us to change environment variables
    testImplementation("org.junit-pioneer:junit-pioneer:1.5.0")
}

// ktlint includes the generated-sources, which includes the classes created by Gradle for these plugins
ktlint {
    enableExperimentalRules.set(true)
    filter {
        // We should be able to just do a wildcard exclude, but it's not working.
        // This solution comes from https://github.com/JLLeitschuh/ktlint-gradle/issues/222#issuecomment-480758375
        exclude { projectDir.toURI().relativize(it.file.toURI()).path.contains("/generated-sources/") }
    }
}

// Publishing
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/projectronin/package-repo")
            credentials {
                username = System.getenv("PACKAGE_USER")
                password = System.getenv("PACKAGE_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
        }
    }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.register("install") {
    dependsOn(tasks.publishToMavenLocal)
}
