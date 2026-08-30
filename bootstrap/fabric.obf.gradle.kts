/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
// bootstrap nodes < 26.1 are obfuscated so they need remapping
plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("oneconfig-bootstrap")
}

dependencies {
    mappings(loom.officialMojangMappings())
}
