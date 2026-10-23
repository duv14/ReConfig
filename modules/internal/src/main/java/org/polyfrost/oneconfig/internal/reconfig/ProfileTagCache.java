/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.reconfig;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** Bounded, asynchronous cache of explicitly public profile tags. Never fetches on the render thread. */
public final class ProfileTagCache {
    public record Tag(String text, String emoji) {
        public Tag {
            if (text == null || !text.matches("[A-Za-z0-9]{1,5}")) throw new IllegalArgumentException("Invalid profile tag");
            if (emoji == null || !List.of("", "rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob").contains(emoji)) emoji = "";
        }
    }
    private record Entry(Tag tag, long expires) {}
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(32, .75f, true);
    private final Set<String> pending = new HashSet<>();
    private final LongSupplier clock;
    private final Executor executor;
    private final Function<String, Tag> fetch;
    private String identity = "";
    private Tag own;
    private long generation;

    public ProfileTagCache(LongSupplier clock, Executor executor, Function<String, Tag> fetch) {
        this.clock = clock; this.executor = executor; this.fetch = fetch;
    }
    public synchronized boolean identity(String uuid) {
        String next = uuid != null && uuid.matches("[a-f0-9]{32}") ? uuid : "";
        if (identity.equals(next)) return false;
        identity = next; own = null; entries.clear(); pending.clear(); generation++;
        return true;
    }
    public synchronized void own(String uuid, Tag tag) { if (identity.equals(uuid)) own = tag; }
    public Tag lookup(String uuid) {
        Tag current;
        final long version;
        synchronized (this) {
            if (identity.isEmpty() || uuid == null || !uuid.matches("[a-f0-9]{32}")) return null;
            if (identity.equals(uuid)) return own;
            Entry entry = entries.get(uuid);
            current = entry == null ? null : entry.tag;
            if (entry != null && entry.expires > clock.getAsLong() || pending.contains(uuid) || pending.size() >= 32) return current;
            pending.add(uuid); version = generation;
        }
        try {
            executor.execute(() -> {
                synchronized (this) { if (version != generation) return; }
                Tag result = null;
                try { result = fetch.apply(uuid); } catch (RuntimeException ignored) { }
                synchronized (this) {
                    if (version != generation) return;
                    pending.remove(uuid);
                    entries.put(uuid, new Entry(result, clock.getAsLong() + (result == null ? 600_000L : 300_000L)));
                    while (entries.size() > 256) entries.remove(entries.keySet().iterator().next());
                }
            });
        } catch (RuntimeException rejected) { synchronized (this) { if (version == generation) pending.remove(uuid); } }
        return current;
    }
}
