/* ReConfig by duv14. See LICENSE-RECONFIG.txt and ATTRIBUTIONS.md. */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.ProfileTagCache;
import org.polyfrost.oneconfig.internal.reconfig.ReConfigProfileTags;
import java.util.List;

/** Extends the native label; never reveals a player whose label Minecraft has hidden. */
public final class ProfileNameTags {
    private static final Identifier EMOJI_ATLAS = Identifier.fromNamespaceAndPath("minecraft", "gui");
    private static final List<String> EMOJIS = List.of("rose", "pray", "cute", "kiss", "kiss-closed", "sob-blue", "sob");
    private ProfileNameTags() {}
    public static void apply(LivingEntity entity, LivingEntityRenderState state, float partial) {
        if (!(entity instanceof Player)) return;
        Minecraft mc = Minecraft.getInstance();
        boolean self = entity == mc.player;
        if (self && ModuleAccess.enabled("nick_hider")) return;
        boolean showOwn = self && !mc.options.hideGui && !mc.options.getCameraType().isFirstPerson()
            && !entity.isInvisible() && !entity.isSpectator();
        if (state.nameTag == null && !showOwn) return;
        ProfileTagCache.Tag tag = ReConfigProfileTags.lookup(entity.getUUID());
        if (tag == null) return;
        Component name = state.nameTag;
        if (name == null) {
            name = entity.getDisplayName();
            state.nameTagAttachment = entity.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, entity.getYRot(partial));
            if (state.nameTagAttachment == null) return;
        }
        state.nameTag = decorate(name, tag);
    }
    public static Component decorate(Component name, ProfileTagCache.Tag tag) {
        var suffix = Component.literal(" [").withColor(0xBBC2D0);
        int emoji = EMOJIS.indexOf(tag.emoji());
        if (emoji >= 0) {
            suffix.append(Component.literal(String.valueOf((char)(0xE000 + emoji)))
                .withStyle(style -> style.withFont(new FontDescription.AtlasSprite(EMOJI_ATLAS,
                    Identifier.fromNamespaceAndPath("reconfig", "tag_emojis/" + tag.emoji()))).withColor(0xFFFFFF)));
            suffix.append(Component.literal(" "));
        }
        suffix.append(Component.literal(tag.text() + "]"));
        return name.copy().append(suffix);
    }
}
