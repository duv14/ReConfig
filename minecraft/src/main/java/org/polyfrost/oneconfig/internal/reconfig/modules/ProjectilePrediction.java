/* ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
 * See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.
 */
package org.polyfrost.oneconfig.internal.reconfig.modules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.polyfrost.oneconfig.internal.reconfig.ModuleAccess;

/** Render-only prediction: vanilla use packets, inventory and server entities remain authoritative. */
public final class ProjectilePrediction {
    private interface Ghost {}
    private record Pending(Projectile entity, Vec3 origin, long created, boolean pearl) {}
    private static final List<Pending> pending = new ArrayList<>();
    private static ClientLevel world;
    private static int nextId = -1_900_000_000;
    private ProjectilePrediction() {}

    public static ItemStack prepare(Player player, InteractionHand hand) {
        if (player != Minecraft.getInstance().player || player.isSpectator()) return ItemStack.EMPTY;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || player.getCooldowns().isOnCooldown(stack)) return ItemStack.EMPTY;
        if ((stack.is(Items.ENDER_PEARL) && ModuleAccess.enabled("pearl_optimizer")) ||
            (stack.is(Items.WIND_CHARGE) && ModuleAccess.enabled("wind_charge_optimizer"))) return stack.copy();
        return ItemStack.EMPTY;
    }

    public static void spawn(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        tick(mc);
        if (stack.isEmpty() || mc.player == null || world == null || pending.size() >= 24) return;
        boolean pearl = stack.is(Items.ENDER_PEARL);
        Projectile entity = pearl ? new GhostPearl(world, mc.player, stack) : new GhostWind(world, mc.player);
        while (world.getEntity(nextId) != null) nextId--;
        entity.setId(nextId--);
        entity.shootFromRotation(mc.player, mc.player.getXRot(), mc.player.getYRot(), 0, 1.5f, 0);
        // Vanilla item-projectile renderers suppress very young, close projectiles.
        entity.tickCount = 3;
        pending.add(new Pending(entity, entity.position(), System.nanoTime(), pearl));
        world.addEntity(entity);
    }

    public static void confirm(Entity serverEntity) {
        if (serverEntity instanceof Ghost || !(serverEntity instanceof Projectile projectile)) return;
        Minecraft mc = Minecraft.getInstance();
        if (world != mc.level || mc.player == null || projectile.getOwner() != mc.player) return;
        boolean pearl = serverEntity instanceof ThrownEnderpearl;
        if (!pearl && !(serverEntity instanceof WindCharge)) return;
        Iterator<Pending> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Pending prediction = iterator.next();
            if (PredictionPolicy.matches(projectile.getOwner() == mc.player, prediction.pearl == pearl,
                prediction.origin.distanceToSqr(serverEntity.position()), System.nanoTime() - prediction.created)) {
                prediction.entity.discard(); iterator.remove();
                return; // Never cancel, move, or replace the authoritative server entity.
            }
        }
    }

    public static void tick(Minecraft mc) {
        if (world != mc.level) {
            pending.forEach(p -> p.entity.discard()); pending.clear(); world = mc.level;
        }
        long now = System.nanoTime();
        pending.removeIf(p -> {
            boolean expired = p.entity.isRemoved() || mc.player == null || now - p.created > PredictionPolicy.TIMEOUT_NANOS ||
                !ModuleAccess.enabled(p.pearl ? "pearl_optimizer" : "wind_charge_optimizer");
            if (expired) p.entity.discard();
            return expired;
        });
    }
    private static boolean hitsBlock(Projectile entity) {
        return entity.level().clip(new ClipContext(entity.position(), entity.position().add(entity.getDeltaMovement()),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity)).getType() != HitResult.Type.MISS;
    }
    private static final class GhostPearl extends ThrownEnderpearl implements Ghost {
        GhostPearl(ClientLevel world, Player player, ItemStack stack) { super(world, player, stack); }
        @Override public void tick() { if (hitsBlock(this)) discard(); else super.tick(); }
        @Override protected void onHit(HitResult hit) { discard(); }
    }
    private static final class GhostWind extends WindCharge implements Ghost {
        GhostWind(ClientLevel world, Player player) { super(player, world, player.getX(), player.getEyeY(), player.getZ()); }
        @Override public void tick() { if (hitsBlock(this)) discard(); else super.tick(); }
        @Override protected void onHit(HitResult hit) { discard(); }
        @Override protected void explode(Vec3 pos) { discard(); }
        @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 4096; }
    }
}
