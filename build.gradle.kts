plugins {
    java
}

group = property("group") as String
version = property("version") as String

fun v(name: String): String = property(name) as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://repo.momirealms.net/releases/") { name = "momirealms" }
    maven("https://repo.extendedclip.com/releases/") { name = "extendedclip" }
}

dependencies {
    // Platform + engine (provided at runtime, never shaded)
    compileOnly("io.papermc.paper:paper-api:${v("paper_api_version")}")
    compileOnly("net.momirealms:custom-fishing:${v("custom_fishing_version")}")

    // Optional integrations (provided by other plugins at runtime)
    compileOnly("me.clip:placeholderapi:${v("placeholderapi_version")}")

    // Runtime libraries: downloaded at load time by FishDexLoader (Paper's library loader),
    // not shaded. HikariCP is compiled against; the JDBC drivers are loaded by class name.
    compileOnly("com.zaxxer:HikariCP:${v("hikaricp_version")}")

    // Tests use a real SQLite database and the Paper API types (YAML, Adventure), but never a running server.
    testImplementation("io.papermc.paper:paper-api:${v("paper_api_version")}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.zaxxer:HikariCP:${v("hikaricp_version")}")
    testRuntimeOnly("org.xerial:sqlite-jdbc:${v("sqlite_version")}")
}

val targetJava = v("java_release").toInt()

java {
    sourceCompatibility = JavaVersion.toVersion(targetJava)
    targetCompatibility = JavaVersion.toVersion(targetJava)
    // Only force a toolchain when the running JVM is older than the target.
    if (JavaVersion.current() < JavaVersion.toVersion(targetJava)) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJava))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJava)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") { expand(props) }
}
