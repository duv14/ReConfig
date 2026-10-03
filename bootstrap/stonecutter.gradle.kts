/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"
stonecutter tasks {
    order("publishModrinth")
}

tasks.register("assembleAllNodes") {
    group = "build"
    description = "Assembles the production jar of every bootstrap node."
    // ReConfig ships only the requested 1.21.11 artifact. Keeping 26.2 in the
    // tree is solely required to describe the checked-in source state.
    dependsOn(stonecutter.tasks.named("assemble") { metadata.version == "1.21.11" })
}

tasks.register("publishMods") {
    group = "publishing"
    description = "Publishes all bootstrap nodes to Modrinth."
    dependsOn(stonecutter.tasks.named("publishMods"))
}

stonecutter {
    parameters {
        constants {
            match(
                current.project.substringAfterLast("-"),
                "fabric",
                "neoforge"
            )
        }
    }
}
