package com.darkun7.runemobs.util;

import org.bukkit.entity.Player;
import java.lang.reflect.Method;
import java.util.UUID;

public class PartyHook {

    public static boolean isPartyActive() {
        return org.bukkit.Bukkit.getPluginManager().getPlugin("Party") != null;
    }

    public static boolean isInParty(Player player) {
        if (!isPartyActive() || player == null)
            return false;
        try {
            Class<?> apiClass = Class.forName("com.darkun7.party.api.PartyAPI");
            Method method = apiClass.getMethod("getPartyPrefix", UUID.class);
            String tag = (String) method.invoke(null, player.getUniqueId());
            return tag != null;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isInSameParty(Player p1, Player p2) {
        if (!isPartyActive() || p1 == null || p2 == null)
            return false;
        try {
            Class<?> apiClass = Class.forName("com.darkun7.party.api.PartyAPI");
            Method method = apiClass.getMethod("getPartyPrefix", UUID.class);
            String tag1 = (String) method.invoke(null, p1.getUniqueId());
            String tag2 = (String) method.invoke(null, p2.getUniqueId());
            if (tag1 != null && tag2 != null) {
                return tag1.equals(tag2);
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
