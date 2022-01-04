plugins {
    id("java")
    id("pl.allegro.tech.build.axion-release") version "1.13.6"
}

group = "com.github.bric3"
version = scmVersion.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:2.0-rc1")
    implementation("org.openjdk.jmc:flightrecorder:8.1.0")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

// Due to https://github.com/gradle/gradle/issues/18426, tasks are not declared in the TaskContainerScope
tasks.withType<JavaExec>().configureEach {
    group = "class-with-main"
    classpath(sourceSets.main.get().runtimeClasspath)

    // Need to set the toolchain https://github.com/gradle/gradle/issues/16791
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}
