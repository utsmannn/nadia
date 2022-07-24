import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
}

group = "com.utsman.nadia"
version = "1.0.20"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("net.lingala.zip4j:zip4j:2.11.1")
    implementation("com.google.code.gson:gson:2.9.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.utsman.nadia.ApplicationKt"
    }

    tasks.withType<Jar> { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.register<Copy>("buildScript") {
    dependsOn("build")
    val jarFile = "$buildDir/libs/${project.name}-${project.version}.jar"
    val intoFile = "$rootDir/dist/libs"
    from(jarFile)
    into(intoFile)

    doLast {
        val file = File("$rootDir/dist", project.name)
        file.writeText("""
            #/usr/bin
            java -jar ${projectDir.absolutePath}/dist/libs/${project.name}-${project.version}.jar ${'$'}@
        """.trimIndent())

        exec {
            commandLine("chmod", "+x", file.absolutePath)
        }
    }

    val scriptLocation = "${projectDir.absolutePath}/dist/${project.name}"
    System.out.println("Script location created on $scriptLocation")
}