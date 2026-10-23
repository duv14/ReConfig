/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.test.profile;

import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.internal.reconfig.ProfileTagCache;
import org.polyfrost.oneconfig.internal.reconfig.modules.ProfileNameTags;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ProfileNameTagsTest {
    @Test void preservesTheNativeNameAndAddsTheSavedFiveCharacterTag() {
        Component name = Component.literal("12duv101");
        Component label = ProfileNameTags.decorate(name, new ProfileTagCache.Tag("ABCDE", ""));
        assertEquals("12duv101", name.getString());
        assertEquals("12duv101 [ABCDE]", label.getString());
    }
    @Test void customEmojiUsesTheGuiSpriteAtlasInsteadOfThe256PixelFontAtlas() {
        Component label = ProfileNameTags.decorate(Component.literal("Player"), new ProfileTagCache.Tag("RCFG", "rose"));
        assertEquals("Player [\uE000 RCFG]", label.getString());
        var emoji = label.getSiblings().getLast().getSiblings().getFirst();
        assertEquals(new FontDescription.AtlasSprite(Identifier.fromNamespaceAndPath("minecraft", "gui"), Identifier.fromNamespaceAndPath("reconfig", "tag_emojis/rose")), emoji.getStyle().getFont());
    }
    @Test void allSevenEmojiSpritesAreRegisteredInTheGuiAtlas() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("assets/minecraft/atlases/gui.json")) {
            assertNotNull(input);
            var sources = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("sources");
            assertEquals(7, sources.size());
            var names = java.util.List.of("rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob");
            for (int i=0; i<names.size(); i++) {
                var entry = sources.get(i).getAsJsonObject();
                assertEquals("minecraft:single", entry.get("type").getAsString());
                assertEquals("reconfig:font/tags/"+names.get(i), entry.get("resource").getAsString());
                assertEquals("reconfig:tag_emojis/"+names.get(i), entry.get("sprite").getAsString());
            }
        }
    }
    @Test void everyTagEmojiGlyphHasADecodableTexture() throws Exception {
        var loader = ProfileNameTagsTest.class.getClassLoader();
        try (var input = loader.getResourceAsStream("assets/reconfig/font/tag_emojis.json")) {
            assertNotNull(input);
            var providers = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("providers");
            assertEquals(7, providers.size());
            for (int i = 0; i < providers.size(); i++) {
                var glyph = providers.get(i).getAsJsonObject();
                assertEquals(String.valueOf((char)(0xE000+i)), glyph.getAsJsonArray("chars").get(0).getAsString());
                String file = glyph.get("file").getAsString().replace("reconfig:", "assets/reconfig/textures/");
                try (var texture = loader.getResourceAsStream(file)) { assertNotNull(texture); assertNotNull(ImageIO.read(texture)); }
            }
        }
    }
}
