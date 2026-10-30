package org.polyfrost.oneconfig.internal.reconfig;

import java.util.HashSet;
import java.util.Set;

/** Persisted mouse codes follow Minecraft's -100 + button convention. */
public final class BindingInput {
    private BindingInput() {}
    public static int mouseCode(int button) { return button >= 0 && button <= 7 ? -100 + button : 0; }
    public static boolean isMouse(int code) { return code >= -100 && code <= -93; }
    public static int mouseButton(int code) { return isMouse(code) ? code + 100 : -1; }
    public static int normalize(int code) { return isMouse(code) || code >= 0 && code <= 512 ? code : 0; }

    /** Keep repeats and releases owned after a press finishes the recording. */
    public static final class CaptureGate {
        private final Set<Integer> held = new HashSet<>();
        public boolean consume(int code, int action, boolean recording) {
            if (held.contains(code)) {
                if (action == 0) held.remove(code);
                return true;
            }
            if (!recording) return false;
            if (action == 1) held.add(code);
            return true;
        }
        public boolean blocksText() { return !held.isEmpty(); }
    }
}
