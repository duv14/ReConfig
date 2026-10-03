/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
allprojects {
    with(tasks) {
        arrayOf("javadocJar", "sourcesJar", "remapSourcesJar").forEach {
            findByName(it)?.enabled = false
        }
    }
}

repositories {
    mavenLocal()
    maven("https://repo.polyfrost.org/snapshots")
}
