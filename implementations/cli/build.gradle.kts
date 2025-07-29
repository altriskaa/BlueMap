import org.gradle.api.plugins.ApplicationPluginConvention
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("bluemap.implementation")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

configure<ApplicationPluginConvention> {
    mainClassName = "de.bluecolored.bluemap.cli.BlueMapCLI"
}

dependencies {
    api(project(":common"))
    api(libs.commons.cli)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "de.bluecolored.bluemap.cli.BlueMapCLI"
    }
}

tasks.named<ShadowJar>("shadowJar") {
    manifest {
        attributes["Main-Class"] = "de.bluecolored.bluemap.cli.BlueMapCLI"
    }
}
