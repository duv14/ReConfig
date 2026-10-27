package org.polyfrost.oneconfig.api.config.v1;

import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.polyfrost.oneconfig.api.config.v1.backend.impl.FileBackend;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.FileSerializer;
import org.polyfrost.oneconfig.api.config.v1.serialize.impl.NightConfigSerializer;
import static org.junit.jupiter.api.Assertions.*;

class OwnWriteWatcherTest {
    @TempDir Path directory;
    static final class Backend extends FileBackend {
        Backend(Path path) { super(path,(FileSerializer<String>[])NightConfigSerializer.ALL); }
        void fileModified() { requestUpdate("settings.json"); }
    }
    @Test void duplicateOwnWriteNotificationsPreserveUnsavedChangesButExternalEditsLoad() throws Exception {
        var backend=new Backend(directory);
        var enabled=Properties.simple("enabled","Enabled",null,false,Boolean.class);
        var tree=Tree.tree("settings.json").put(enabled);
        backend.register(tree);
        backend.save(tree);
        String saved=Files.readString(directory.resolve("settings.json"));
        enabled.setAs(true);
        // Windows can deliver multiple notifications for one atomic replacement.
        backend.fileModified(); backend.fileModified();
        assertEquals(Boolean.TRUE,enabled.get(),"our own save must not overwrite a newer in-memory edit");
        enabled.setAs(false);
        Files.writeString(directory.resolve("settings.json"),"{\"enabled\": true, \"external\": true}");
        backend.fileModified();
        assertEquals(Boolean.TRUE,enabled.get(),"a genuinely external edit must still reload");
        Files.writeString(directory.resolve("settings.json"),saved);
        backend.fileModified();
        assertEquals(Boolean.FALSE,enabled.get(),"an external restore of our original saved bytes must reload too");
    }
}
