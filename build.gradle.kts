import com.mythicalnetwork.gradle.Dependencies
import com.mythicalnetwork.gradle.Versions
import com.mythicalnetwork.gradle.ProjectInfo
import com.mythicalnetwork.gradle.Repos

plugins {
    id("java")
    id("io.github.juuxel.loom-quiltflower") version ("1.8.+")
    id("org.quiltmc.loom") version("1.2.+")
    kotlin("jvm") version ("1.8.0")
    kotlin("kapt") version("1.8.20")
}

group = ProjectInfo.GROUP
version = ProjectInfo.VERSION
base {
    archivesName.set("MythicalDaycareModule")
}

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
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")?.let { include(it) }
    minecraft("com.mojang:minecraft:${Versions.MINECRAFT}")
    mappings(loom.layered {
        mappings(Dependencies.QUILT_MAPPINGS)
        officialMojangMappings()
    })
    for(dep in Dependencies.CORE_DEPS){
        if(dep.equals("owo-lib")){
            modImplementation(dep)
        } else {
            include(dep)?.let {
                if(!dep.contains("owo-sentinel")){
                    modImplementation(it)
                }
            }
        }
        if(dep.contains("owo-lib")){
            annotationProcessor(dep)
            kapt(dep)
        }
    }
    kapt("org.ow2.asm:asm:9.3")
    modImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    include("dev.lightdream:database-manager:5.0.9")?.let { implementation(it) }
    include("dev.lightdream:file-manager:2.7.2")?.let { implementation(it) }
    include("dev.lightdream:lambda:4.1.14")?.let { implementation(it) }
    include("dev.lightdream:logger:3.3.11")?.let { implementation(it) }

    include("org.reflections:reflections:0.10.2")?.let { implementation(it) }

    include("org.hibernate.common:hibernate-commons-annotations:6.0.6.Final")
    include("org.hibernate.orm:hibernate-core:6.2.0.Final")

    testImplementation(Dependencies.JUNIT_JUPITER_API)
    testRuntimeOnly(Dependencies.JUNIT_JUPITER_ENGINE)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}