package org.polyfrost.oneconfig.internal.reconfig;

import java.util.function.LongSupplier;

/** Shared by every social feature so failed ownership proofs never hammer Mojang. */
public final class EnrollmentRetry {
    private final LongSupplier clock;
    private long retryAt;
    private int failures;
    private boolean limited;
    public EnrollmentRetry() { this(System::currentTimeMillis); }
    public EnrollmentRetry(LongSupplier clock) { this.clock = clock; }
    public synchronized void beforeAttempt() {
        if (clock.getAsLong() < retryAt) throw new IllegalStateException(message());
    }
    public synchronized IllegalStateException failed(Throwable failure) {
        limited = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            String text = String.valueOf(cause.getMessage()).toLowerCase(java.util.Locale.ROOT);
            if (text.contains("ratelimiter") || text.contains("429") || text.contains("rate limit")) limited = true;
        }
        failures = Math.min(failures + 1, 5);
        long delay = Math.min(900_000L, (limited ? 300_000L : 60_000L) << (failures - 1));
        retryAt = clock.getAsLong() + delay;
        return new IllegalStateException(message(), failure);
    }
    private String message() {
        long seconds = Math.max(1, (retryAt - clock.getAsLong() + 999) / 1000);
        return (limited ? "Minecraft verification is rate limited. " : "Minecraft account verification is temporarily unavailable. ")
                + "ReConfig will retry automatically in " + seconds + "s. You can stay signed in.";
    }
    public synchronized void succeeded() { reset(); }
    public synchronized void reset() { failures = 0; retryAt = 0; limited = false; }
}
