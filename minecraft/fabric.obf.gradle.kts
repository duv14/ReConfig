/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
@file:Suppress("UnstableApiUsage")
plugins {
    id("net.fabricmc.fabric-loom-remap")
    `oneconfig-fabric`
}

dependencies {
    mappings(loom.officialMojangMappings())

    modRuntimeOnly(rootProject.fileTree("minecraft/run/extra_mods").include("*.jar"))
}
