/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
plugins {
    `oneconfig-neoforge`
}

tasks.named("build") {
    enabled = false
}
