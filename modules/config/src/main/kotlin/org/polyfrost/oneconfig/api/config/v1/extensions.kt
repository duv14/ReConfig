/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.config.v1

inline fun <reified T> Tree.getProp(id: String): Property<T>? {
    return this.getProp(id) as Property<T>?
}
