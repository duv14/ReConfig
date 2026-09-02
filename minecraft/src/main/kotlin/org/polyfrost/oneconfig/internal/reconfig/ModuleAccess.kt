/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig

/** Small Java/mixin-safe bridge to persisted module state. */
object ModuleAccess {
    @JvmStatic fun enabled(id: String): Boolean = ModuleCatalog.byId(id)?.enabled == true
    @JvmStatic fun number(id: String, settingId: String, fallback: Float): Float {
        val module = ModuleCatalog.byId(id) ?: return fallback
        val setting = module.settings.firstOrNull { it.id == settingId } ?: return fallback
        return module.value(setting).toFloatOrNull() ?: fallback
    }
    @JvmStatic fun choice(id: String, settingId: String, fallback: String): String {
        val module = ModuleCatalog.byId(id) ?: return fallback
        val setting = module.settings.firstOrNull { it.id == settingId } ?: return fallback
        return module.value(setting)
    }
}
