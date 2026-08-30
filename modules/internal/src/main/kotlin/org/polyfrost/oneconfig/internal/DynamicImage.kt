/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal

import org.jetbrains.skia.Image
import java.io.InputStream

class DynamicImage(val path: String, private val stream: InputStream) {
    val image: Image by lazy { Image.makeFromEncoded(stream.readBytes()) }
}
