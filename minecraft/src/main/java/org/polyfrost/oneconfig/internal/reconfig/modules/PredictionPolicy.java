/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

public final class PredictionPolicy {
    public static final long TIMEOUT_NANOS = 5_000_000_000L;
    private PredictionPolicy() {}
    public static boolean matches(boolean sameOwner, boolean sameKind, double distanceSquared, long ageNanos) {
        return sameOwner && sameKind && distanceSquared >= 0 && distanceSquared < 16 && ageNanos >= 0 && ageNanos <= TIMEOUT_NANOS;
    }
}
