package com.catadmirer.infuseSMP.managers;

import com.catadmirer.infuseSMP.EffectIds;
import com.catadmirer.infuseSMP.Infuse;
import com.catadmirer.infuseSMP.effects.Ender;
import com.catadmirer.infuseSMP.effects.InfuseEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ParticleManager {
    public static void spawnEffectParticles(Player player, InfuseEffect effect) {
        if (effect == null)
            return;

        // Handling special particles for ender effect
        // TODO: Decide whether or not to keep this
        if (effect.getId() == EffectIds.ENDER) {
            player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, player.getLocation().add(0, 1, 0), 32, 0.3, 0.5,
                    0.3, 0);
            return;
        }

        player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, player.getLocation().add(0, 1, 0), 2, 0.3, 0.5, 0.3,
                0.1, Color.fromARGB(effect.getPotionColor().getRGB()));
    }

    public static void spawnCursedParticles(Player player) {
        if (!Ender.cursedPlayers.contains(player.getUniqueId())) return;

        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
    }

    /**
     * Spawns a cloud of effect particles around the player.
     *
     * @param player The player to spawn entity effect particles on.
     * @param color  The color the particles should be.
     */
    public static void spawnEffectCloud(Player player, Color color) {
        player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.6, 0.5, 0,
                color);
    }

    public static void drawLine(Location start, Location end) {
        drawLine(start, end, 5, new DustOptions(Color.WHITE, 1));
    }

    public static void drawLine(Location start, Location end, int count) {
        drawLine(start, end, count, new DustOptions(Color.WHITE, 1));
    }

    public static void drawLine(Location start, Location end, DustOptions dustOptions) {
        drawLine(start, end, 5, dustOptions);
    }

    public static void drawLine(Location start, Location end, int count, DustOptions dustOptions) {
        if (!start.getWorld().equals(end.getWorld())) {
            Infuse.LOGGER.debug("Cannot draw lines between two worlds!");
            return;
        }

        Location diff = end.subtract(start);
        int points = (int) (diff.length() * 10);
        Location step = diff.multiply(1.0 / points);
        for (int i = 0; i < points; i++) {
            start.getWorld().spawnParticle(Particle.DUST, start, count, 0, 0, 0, 0, dustOptions);
            start.add(step);
        }
        start.getWorld().spawnParticle(Particle.DUST, end, count, 0, 0, 0, 0, dustOptions);
    }

    public static void dropEffect(Infuse plugin, boolean bottomToTop, @NotNull InfuseEffect effect, Location location) {
        final Location base = location.add(0, bottomToTop ? 0 : 2, 0);
        final World world = location.getWorld();
        Color color = Color.fromRGB(effect.getPotionColor().getRGB() & 0xFFFFFF);
        final Particle.DustOptions dust = new Particle.DustOptions(color, 0.7F);
        final int points = 16;
        final double radius = 0.6;
        (new BukkitRunnable() {
            double y = 0;

            public void run() {
                if (this.y > 2) {
                    this.cancel();
                } else {
                    double ringY = bottomToTop ? this.y : 2 - this.y;

                    for(int i = 0; i < points; ++i) {
                        double angle = Math.PI * 2 * i / points;
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        world.spawnParticle(Particle.DUST, base.clone().add(x, ringY, z), 0, 0, 0, 0, 1, dust);
                    }

                    this.y += 0.15;
                }
            }
        }).runTaskTimer(plugin, 0, 1);
        world.playSound(base, Sound.ENTITY_TURTLE_EGG_BREAK, 1.3F, 1.2F);
    }
}