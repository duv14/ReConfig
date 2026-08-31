/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * Adapted from duv14's Hitbox Categories source; see ATTRIBUTIONS.md.
 */
package org.polyfrost.oneconfig.internal.mixin.reconfig;

import net.minecraft.client.Minecraft;
import org.polyfrost.oneconfig.internal.reconfig.CombatRepository;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;
import org.polyfrost.oneconfig.internal.reconfig.combat.*;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Replaces vanilla F3+B rendering for remote players while preserving the
 * vanilla-style eye/look/riding helper gizmos.
 */
@Mixin(EntityHitboxDebugRenderer.class)
public abstract class Mixin_ReConfigHitboxes {
    @Unique private static final int WHITE = 0xFFFFFFFF;
    @Unique private static final int EYE_RED = 0xFFFF0000;
    @Unique private static final int LOOK_BLUE = 0xFF0000FF;
    @Unique private static final int RIDING_YELLOW = 0xFFFFFF00;

    @Unique private static final float BODY_LINE_WIDTH = 2.5F;
    @Unique private static final float AUX_LINE_WIDTH = 2.5F;

    @Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true)
    private void hitboxCategories$drawPlayerHitbox(
            Entity entity,
            float tickProgress,
            boolean inLocalServer,
            CallbackInfo ci
    ) {
        if (!ModuleAccess.enabled("hitbox") || !(entity instanceof Player target)) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        Player localPlayer = client.player;
        if (localPlayer == null || target == localPlayer || target.getUUID().equals(localPlayer.getUUID())) {
            return;
        }

        ci.cancel();

        HitboxCategoriesConfig config = CombatRepository.getHitboxes();
        Optional<HitboxCategory> category = hitboxCategories$category(target);
        int baseColor = category.map(HitboxCategory::getArgb).orElse(WHITE);
        boolean aimedAndInRange = localPlayer.isAlive() && !localPlayer.isSpectator() && target.isAlive() && !target.isSpectator() && target.isAttackable() && target.isPickable() && localPlayer.isWithinAttackRange(target.getBoundingBox(), 0.0D) && client.hitResult instanceof EntityHitResult hit && hit.getEntity() == target;
        boolean hurtFlashing = category.isPresent() && CombatRepository.getFlashes().isFlashing(target.getUUID(), System.currentTimeMillis());
        int hurtColor = category.map(HitboxCategory::getHurtArgb).orElse(baseColor);
        int rangeColor = config.getRangeArgb();

        int bodyColor = HitboxColorResolver.resolve(
                baseColor,
                rangeColor,
                hurtColor,
                aimedAndInRange,
                hurtFlashing
        );
        // Only the body hitbox gets the one-second hurt flash. The center point
        // can still show the normal/range state, but never the hurt state.
        int pointColor = HitboxColorResolver.resolve(
                baseColor,
                rangeColor,
                baseColor,
                aimedAndInRange,
                false
        );

        float configuredWidth = BODY_LINE_WIDTH * config.getHitboxThickness();
        float bodyWidth = DistanceLineWidth.forDistance(configuredWidth, localPlayer.distanceTo(target));

        Vec3 currentPos = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        Vec3 lerpedPos = entity.getPosition(tickProgress);
        Vec3 renderDelta = lerpedPos.subtract(currentPos);
        AABB bodyBox = entity.getBoundingBox().move(renderDelta.x, renderDelta.y, renderDelta.z);

        Gizmos.cuboid(bodyBox, GizmoStyle.stroke(bodyColor, bodyWidth));
        Gizmos.point(lerpedPos, pointColor, 2.0F);

        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            float halfWidth = Math.min(vehicle.getBbWidth(), entity.getBbWidth()) / 2.0F;
            float height = 0.0625F;
            Vec3 ridingPos = vehicle.getPassengerRidingPosition(entity).add(renderDelta);
            Gizmos.cuboid(
                    new AABB(
                            ridingPos.x - halfWidth, ridingPos.y, ridingPos.z - halfWidth,
                            ridingPos.x + halfWidth, ridingPos.y + height, ridingPos.z + halfWidth
                    ),
                    GizmoStyle.stroke(RIDING_YELLOW, AUX_LINE_WIDTH)
            );
        }

        float eyeHalfThickness = 0.01F;
        float eyeHeight = entity.getEyeHeight(entity.getPose());
        double eyeY = bodyBox.minY + eyeHeight;
        Gizmos.cuboid(
                new AABB(
                        bodyBox.minX, eyeY - eyeHalfThickness, bodyBox.minZ,
                        bodyBox.maxX, eyeY + eyeHalfThickness, bodyBox.maxZ
                ),
                GizmoStyle.stroke(EYE_RED, AUX_LINE_WIDTH)
        );

        Vec3 eyePos = lerpedPos.add(0.0D, eyeHeight, 0.0D);
        Vec3 lookDirection = entity.getViewVector(tickProgress);
        Gizmos.line(eyePos, eyePos.add(lookDirection.scale(2.0D)), LOOK_BLUE, AUX_LINE_WIDTH);
    }

    @Unique
    private static Optional<HitboxCategory> hitboxCategories$category(Player target) {
        String username = target.getName().getString();
        return CombatRepository.getHitboxes().findCategoryForPlayer(username);
    }
}
