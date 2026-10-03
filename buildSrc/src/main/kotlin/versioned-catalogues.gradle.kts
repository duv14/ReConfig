/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.jetbrains.kotlin.gradle.plugin.extraProperties

private val stonecutter = project.extensions.getByName("stonecutter") as StonecutterBuildExtension

entries[project] = project.getForwardingVersionCatalog(stonecutter.current)
