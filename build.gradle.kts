import com.mythicalnetwork.gradle.Dependencies
import com.mythicalnetwork.gradle.Versions
import com.mythicalnetwork.gradle.ProjectInfo
import com.mythicalnetwork.gradle.Repos
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("io.github.juuxel.loom-quiltflower") version ("1.8.+")
    id("org.quiltmc.loom") version("1.2.+")
    kotlin("jvm") version ("1.8.0")
    kotlin("kapt") version("1.8.20")
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

group = ProjectInfo.GROUP
version = ProjectInfo.VERSION
base {
    archivesName.set("MythicalDaycareModule")
}

val shade: Configuration by configurations.creating
listOf(configurations.implementation)
    .forEach { it { extendsFrom(shade) } }

loom {
    mixin {
        defaultRefmapName.set("mixins.${project.name}.refmap.json")
    }
    interfaceInjection {
        enableDependencyInterfaceInjection.set(true)
    }
}

repositories {
    mavenCentral()
    for(repo in Repos.BASE){
        if(repo.contains("sonatype.org")){
            maven(url = repo){
                name = "sonatype-oss-snapshots1"
                mavenContent { snapshotsOnly() }
            }
        } else {
            maven(url = repo)
        }
    }
    maven ( "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://cursemaven.com/")
}


dependencies {
    minecraft("com.mojang:minecraft:${Versions.MINECRAFT}")
    mappings(loom.layered {
        mappings(Dependencies.QUILT_MAPPINGS)
        officialMojangMappings()
    })

    for(dep in Dependencies.CORE_DEPS){
        modImplementation(dep)
        if(dep.contains("owo-lib")){
            annotationProcessor(dep)
            kapt(dep)
        }
    }

    for (dep in Dependencies.INCLUDE_DEPS) {
        modImplementation(dep)
        shade(dep)
    }

    kapt("org.ow2.asm:asm:9.3")
    modImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    shade("dev.lightdream:database-manager:5.0.9")?.let { implementation(it) }
    shade("dev.lightdream:file-manager:2.7.2")?.let { implementation(it) }
    shade("dev.lightdream:lambda:4.1.14")?.let { implementation(it) }
    shade("dev.lightdream:logger:3.3.11")?.let { implementation(it) }

    shade("org.hibernate.common:hibernate-commons-annotations:6.0.6.Final")
    shade("org.hibernate.orm:hibernate-core:6.2.0.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")?.let { include(it) }

    testImplementation(Dependencies.JUNIT_JUPITER_API)
    testRuntimeOnly(Dependencies.JUNIT_JUPITER_ENGINE)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

val shadowJar by tasks.getting(ShadowJar::class) {
    configurations = listOf(shade)
    dependencies {
        exclude(dependency("org.slf4j:.*:.*"))
    }
    relocate("com.mysql", "com.mythicalnetwork.mythicaldaycare.libs")
    relocate("org.mariadb", "com.mythicalnetwork.mythicaldaycare.libs")
    relocate("org.apache", "com.mythicalnetwork.mythicaldaycare.libs")
    relocate("org.sqlite", "com.mythicalnetwork.mythicaldaycare.libs")
    relocate("org.h2", "com.mythicalnetwork.mythicaldaycare.libs")
    relocate("com.google", "com.mythicalnetwork.mythicaldaycare.libs")
}

val remapShadowJar = tasks.register<net.fabricmc.loom.task.RemapJarTask>("remapShadowJar") {
    dependsOn(shadowJar)
    archiveClassifier.set("")
    input.set(shadowJar.archiveFile)
    addNestedDependencies.set(true)
}

tasks.build.configure {
    dependsOn(remapShadowJar)
}