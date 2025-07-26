plugins {
    bluemap.base
    id("org.sonarqube") version "6.0.1.5171"

}

dependencies {
    api ( "de.bluecolored:bluemap-api:2.7.4" )

    api ( libs.aircompressor )
    api ( libs.bluenbt )
    api ( libs.caffeine )
    api ( libs.commons.dbcp2 )
    api ( libs.configurate.hocon )
    api ( libs.configurate.gson )
    api ( libs.lz4 )

    compileOnly ( libs.jetbrains.annotations )
    compileOnly ( libs.lombok )

    annotationProcessor ( libs.lombok )

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)


    // tests
    testImplementation ( libs.junit.core )
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly ( libs.junit.engine )
    testRuntimeOnly ( libs.lombok )
    testAnnotationProcessor ( libs.lombok )
}

tasks.register("zipResourceExtensions", type = Zip::class) {
    from(fileTree("src/main/resourceExtensions"))
    archiveFileName = "resourceExtensions.zip"
    destinationDirectory = file("src/main/resources/de/bluecolored/bluemap/")
}

tasks.processResources {
    dependsOn("zipResourceExtensions")

    from("src/main/resources") {
        include("de/bluecolored/bluemap/version.json")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        expand (
            "version" to project.version,
            "gitHash" to gitHash() + if (gitClean()) "" else " (dirty)",
        )
    }
}

tasks.getByName("sourcesJar") {
    dependsOn("zipResourceExtensions")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "bluemap-${project.name}"
            version = project.version.toString()

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionResult()
                }
            }
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "BlueMap")
        property("sonar.projectName", "BlueMap")
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.token", "sqp_7570465a6be1843aa7780aa375a811ac882f2b55") // Ganti dengan token asli
    }
}
