package com.darkun7.runemobs.listeners;

import com.darkun7.runemobs.RuneMobs;
import com.darkun7.runemobs.util.PartyHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Projectile;
import org.bukkit.projectiles.ProjectileSource;

public class PartyDamageListener implements Listener {

    private final RuneMobs plugin;

    public PartyDamageListener(RuneMobs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim))
            return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj) {
            ProjectileSource source = proj.getShooter();
            if (source instanceof Player p) {
                attacker = p;
            }
        }

        if (attacker != null && !attacker.equals(victim)) {
            // Check if they are in the same party
            if (PartyHook.isInSameParty(attacker, victim)) {
                // Friendly fire! Cancel the damage.
                event.setCancelled(true);
                attacker.sendMessage("§cYou cannot damage your party members!");
            }
        }
    }
}
