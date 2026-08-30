/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */

allprojects {
    // Loom remaps the normal jar, including its notices. Adding them to
    // remapJar as well creates duplicate archive entries.
    tasks.withType<org.gradle.jvm.tasks.Jar>().matching { it.name == "jar" }.configureEach {
        from(rootProject.file("LICENSE")) { into("META-INF/reconfig-notices") }
        from(rootProject.file("LICENSE-RECONFIG.txt")) { into("META-INF/reconfig-notices") }
        from(rootProject.file("ATTRIBUTIONS.md")) { into("META-INF/reconfig-notices") }
        from(rootProject.file("THIRD_PARTY_NOTICES.md")) { into("META-INF/reconfig-notices") }
        from(rootProject.file("FILE_ATTRIBUTIONS.tsv")) { into("META-INF/reconfig-notices") }
    }
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/releases")
    }
}

// Each bootstrap node JiJ's its platform jar, modules, and Compose runtime.
val bootstrapNodes = subprojects.filter { it.path == ":bootstrap:1.21.11-fabric" }

// Sync rather than Copy because node build/libs keeps jars of previously built versions
tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds and collects the standalone ReConfig jar into build/libs."

    dependsOn(bootstrapNodes.map { "${it.path}:assemble" })

    // loom keeps the unmapped jar in build/devlibs so build/libs holds only the shippable one
    // version filter skips stale jars from earlier project versions
    from(bootstrapNodes.map { it.layout.buildDirectory.dir("libs") }) {
        include("*-${rootProject.version}.jar")
    }
    into(layout.buildDirectory.dir("libs"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    doFirst {
        if (bootstrapNodes.isEmpty()) {
            throw GradleException("No bootstrap nodes were registered — check the stonecutter tree in settings.gradle.kts.")
        }
    }

    doLast {
        val target = destinationDir
        logger.lifecycle("Collected ${target.listFiles { f -> f.extension == "jar" }?.size ?: 0} jars into $target")
    }
}
