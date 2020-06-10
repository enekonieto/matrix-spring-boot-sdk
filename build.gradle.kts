import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    base
    id("org.springframework.boot") version "2.3.0.RELEASE" apply false
    id("io.spring.dependency-management") version "1.0.9.RELEASE" apply false
    kotlin("jvm") version "1.3.72"
    kotlin("kapt") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72" apply false
}

allprojects {
    apply(plugin = "kotlin")

    group = "net.folivo"
    version = "0.2.7.RELEASE"
    java.sourceCompatibility = JavaVersion.VERSION_11

    repositories {
        mavenCentral()
    }

    extra["isReleaseVersion"] = version.toString().endsWith("RELEASE")
}
subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

        testImplementation("com.ninja-squad:springmockk:2.0.1")

        testImplementation("org.springframework.boot:spring-boot-starter-test") {
            exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
            exclude(group = "org.mockito", module = "mockito-core")
            exclude(group = "org.mockito", module = "mockito-junit-jupiter")
        }
    }

    the<DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }

    tasks.getByName<Jar>("jar") {
        enabled = true
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<BootJar> {
        enabled = false
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "11"
        }
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { project.extra["isReleaseVersion"] as Boolean }
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
