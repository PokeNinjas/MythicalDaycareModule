import com.mythicalnetwork.gradle.Dependencies
import com.mythicalnetwork.gradle.Versions
import com.mythicalnetwork.gradle.ProjectInfo
import com.mythicalnetwork.gradle.Repos

plugins {
    id("java")
    id("io.github.juuxel.loom-quiltflower") version ("1.8.+")
    id("org.quiltmc.loom") version("1.0.+")
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
    maven( "https://repo.lightdream.dev/")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
    maven("https://maven.impactdev.net/repository/development/")
    maven("https://cursemaven.com/")
}


dependencies {
    minecraft("com.mojang:minecraft:${Versions.MINECRAFT}")
//    implementation(project(mapOf("path" to ":MythicalContentModule")))
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
    modImplementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    compileOnly(project(":MythicalContentModule"))
    include("dev.lightdream:database-manager:5.0.1")?.let { modImplementation(it) }
    include("dev.lightdream:file-manager:2.6.0")?.let { modImplementation(it) }
    include("dev.lightdream:lambda:4.0.0")?.let { modImplementation(it) }
    include("dev.lightdream:logger:3.1.0")?.let { modImplementation(it) }

    include("org.reflections:reflections:0.10.2")?.let { modImplementation(it) }

    include("org.hibernate.common:hibernate-commons-annotations:6.0.6.Final")
    include("org.hibernate.orm:hibernate-core:6.2.0.Final")

    testImplementation(Dependencies.JUNIT_JUPITER_API)
    testRuntimeOnly(Dependencies.JUNIT_JUPITER_ENGINE)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}