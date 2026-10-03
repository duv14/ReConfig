/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("fabric") {
            from(files("../gradle/fabric.versions.toml"))
        }
        create("neoforge") {
            from(files("../gradle/neoforge.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
