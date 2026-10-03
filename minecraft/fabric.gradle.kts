/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
plugins {
    id("net.fabricmc.fabric-loom")
    `oneconfig-bridge` // creates the modImplementation and friends configurations
    `oneconfig-fabric`
}

dependencies {
    if (versionedCatalog.has("modmenu"))
    implementation(versionedCatalog["modmenu"])
}
