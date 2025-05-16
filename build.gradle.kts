plugins {
    id("java")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.github.antoj2.blockfreq.Main")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Avoid duplicate resource issues
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) // Include dependencies
}

group = "com.github.antoj2"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("ens:nbt:0.1")
}

tasks.test {
    useJUnitPlatform()
}