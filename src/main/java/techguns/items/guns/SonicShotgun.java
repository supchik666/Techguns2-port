package techguns.items.guns;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import techguns.entities.projectiles.GenericProjectile;
import techguns.entities.projectiles.SonicShotgunProjectile;
import techguns.items.guns.ammo.AmmoType;

/** Fires one blast straight ahead plus rings of 5 and 10 around it (1.12 SonicShotgun). */
public class SonicShotgun extends GenericGun {
	public SonicShotgun(AmmoType ammoType, boolean semiAuto, int minFiretime, int clipsize, int reloadtime, float damage,
			Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound, int ttl, float accuracy) {
		super(ammoType, semiAuto, minFiretime, clipsize, reloadtime, damage, fireSound, reloadSound, ttl, accuracy);
	}

	/** All 16 blasts share one hit set, so an entity is hit once per shot; the center one draws the shockwave. */
	@Override
	protected void spawnProjectile(Level level, Player player, GenericProjectile.Kind kind, float spread, float offset, float side, Entity target) {
		SonicShotgunProjectile main = (SonicShotgunProjectile) createProjectile(level, player, kind, 0f, offset, side);
		main.mainProjectile = true;
		level.addFreshEntity(main);
		int count = 5;
		float ringSpread = 0.075f;
		for (int j = 1; j <= 2; j++) {
			float angle = (float) (Math.PI * 2.0 / (count * j));
			for (int i = 0; i < count * j; i++) {
				SonicShotgunProjectile ring = (SonicShotgunProjectile) createProjectile(level, player, kind, 0f, 0f, side);
				ring.entitiesHit = main.entitiesHit;
				Vec3 dir = new Vec3(1, 0, 0).yRot(ringSpread * j).xRot(angle * i).zRot(player.getXRot() * Mth.DEG_TO_RAD)
						.yRot((-player.getYHeadRot() - 90.0f) * Mth.DEG_TO_RAD);
				ring.setDeltaMovement(dir.scale(speed));
				if (offset > 0) ring.shiftForward(offset / speed);
				level.addFreshEntity(ring);
			}
		}
	}
}
