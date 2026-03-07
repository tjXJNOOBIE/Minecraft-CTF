plugins {
    java
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.16.1")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("net.bytebuddy:byte-buddy:1.17.5")
    testRuntimeOnly("net.bytebuddy:byte-buddy-agent:1.17.5")
}

sourceSets {
    create("serviceLoaderTest") {
        java.srcDir("src/serviceLoaderTest/java")
        resources.srcDir("src/serviceLoaderTest/resources")
        compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
        runtimeClasspath += output + compileClasspath
    }
}

configurations.named("serviceLoaderTestImplementation") {
    extendsFrom(configurations["testImplementation"])
}

configurations.named("serviceLoaderTestRuntimeOnly") {
    extendsFrom(configurations["testRuntimeOnly"])
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Test>("serviceLoaderTest") {
    description = "Runs isolated tests for ServiceLoader."
    group = "verification"
    testClassesDirs = sourceSets["serviceLoaderTest"].output.classesDirs
    classpath = sourceSets["serviceLoaderTest"].runtimeClasspath
    useJUnitPlatform()
}

tasks.processResources {
    from(rootProject.file("tools/sim")) {
        into("sim-bundle/tools/sim")
        exclude("local-run.out", "local-run.err")
    }
    from(rootProject.file("bot-testing/src")) {
        into("sim-bundle/bot-testing/src")
    }
    from(rootProject.file("package.json")) {
        into("sim-bundle")
    }
    from(rootProject.file("package-lock.json")) {
        into("sim-bundle")
    }
    from(rootProject.file("tsconfig.json")) {
        into("sim-bundle")
    }
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
