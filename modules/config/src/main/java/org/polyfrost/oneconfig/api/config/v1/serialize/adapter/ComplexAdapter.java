/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.api.config.v1.serialize.adapter;

import java.util.Map;

public abstract class ComplexAdapter<Type> extends Adapter<Type, Map<String, Object>> {
    @SuppressWarnings("unchecked")
    @Override
    public final Class<Map<String, Object>> getOutputClass() {
        return (Class<Map<String, Object>>) (Class<?>) (Map.class);
    }
}
