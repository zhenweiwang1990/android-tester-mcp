plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.17.4"

}

group = "ai.gbox"
version = "1.0.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.2.1")
    type.set("IC")
    
    plugins.set(listOf(
        "gradle",
        "java",
        "android"
    ))
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.mockito:mockito-core:5.1.1")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("999.*")
        
        pluginDescription.set("""
            Gbox Android Studio Plugin provides APIs to programmatically control Android app execution.
            Features include start, stop, rerun, and debug operations that can be controlled via HTTP.
        """)
        
        changeNotes.set("""
            <em>Version 1.0.0</em>
            <ul>
                <li>Initial release</li>
                <li>Support for start/stop/rerun/debug operations</li>
                <li>HTTP API endpoints for external control</li>
                
            </ul>
        """)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

tasks.test {
    useJUnitPlatform()
}

