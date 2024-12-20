plugins {
    id("java")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "org.example.Main")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Avoid duplicate resource issues
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) // Include dependencies
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.github.ensgijs:ens-nbt:0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}