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

// 26.2 is retained only as Stonecutter's source-shape node. ReConfig ships
// 1.21.11, and compiling the 26.2 binary node would mix incompatible Minecraft
// APIs into a normal `build` invocation.
val unsupportedBinaryNodes = listOf(
    ":minecraft:26.2-fabric",
    ":bootstrap:26.2-fabric",
)
unsupportedBinaryNodes.forEach { path ->
    project(path).tasks.configureEach { enabled = false }
}

// Each bootstrap node JiJ's its platform jar, modules, and Compose runtime.
val bootstrapNodes = subprojects.filter { it.path == ":bootstrap:1.21.11-fabric" }

// Sync rather than Copy because node build/libs keeps jars of previously built versions
val isolatedRelease = tasks.register<dev.duv14.build.IsolateComposeRuntime>("isolateReleaseRuntime") {
    group = "build"
    description = "Privately packages Compose so other mods cannot replace ReConfig's UI runtime."
    dependsOn(bootstrapNodes.map { "${it.path}:assemble" })
    inputJar.set(project(":bootstrap:1.21.11-fabric").layout.buildDirectory.file("libs/ReConfig-1.21.11-fabric-${rootProject.version}.jar"))
    outputJar.set(layout.buildDirectory.file("isolated/ReConfig-${rootProject.version}.jar"))
}

tasks.register<Sync>("buildAndCollect") {
    group = "build"
    description = "Builds and collects the standalone ReConfig jar into build/libs."
    dependsOn(isolatedRelease)
    from(isolatedRelease.flatMap { it.outputJar })
    into(layout.buildDirectory.dir("libs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    doLast { logger.lifecycle("Collected isolated ReConfig release into $destinationDir") }
}
