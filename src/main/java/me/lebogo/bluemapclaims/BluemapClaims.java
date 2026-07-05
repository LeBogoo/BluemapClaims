package me.lebogo.bluemapclaims;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Logger;

public final class BluemapClaims extends JavaPlugin {
    Logger logger = getLogger();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        long updateInterval = getConfig().getLong("updateInterval", 300);

        // Plugin startup logic
        BlueMapAPI.onEnable(api -> {
            logger.info("Bluemap instance found!");
            if (GriefPrevention.instance == null) {
                logger.warning("Failed to find GriefPrevention instance. Disabling plugin.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            logger.info("GriefPrevention instance found!");

            // sync claims every 5 minutes
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> syncClaims(api, GriefPrevention.instance), 0, updateInterval * 20);
        });
    }


    private void syncClaims(BlueMapAPI blueMapAPI, GriefPrevention griefPrevention) {
        Collection<Claim> claims = griefPrevention.dataStore.getClaims();


        boolean enableUpdateLogs = getConfig().getBoolean("enableUpdateLogs", false);

        if (enableUpdateLogs) {
            logger.info("Found " + claims.size() + " claims");
            logger.info("Syncing claims...");
        }

        Map<String, BlueMapWorld> blueMapWorlds = new HashMap<>();

        Collection<BlueMapWorld> worlds = blueMapAPI.getWorlds();
        for (BlueMapWorld world : worlds) {
            String worldName = world.getId().split("#")[1];
            blueMapWorlds.put(worldName, world);
        }

        Map<String, List<Claim>> claimsByWorld = new HashMap<>();

        for (Claim claim : claims) {
            World world = claim.getLesserBoundaryCorner().getWorld();

            if (world == null) continue;
            String name = world.getKey().toString();

            List<Claim> claimsInWorld = claimsByWorld.computeIfAbsent(name, k -> new ArrayList<>());
            claimsInWorld.add(claim);
        }

        for (String worldName : blueMapWorlds.keySet()) {
            BlueMapWorld blueMapWorld = blueMapWorlds.get(worldName);
            Collection<BlueMapMap> maps = blueMapWorld.getMaps();

            MarkerSet claimsMarkerSet = MarkerSet.builder()
                    .label("Claims")
                    .build();

            if(claimsByWorld.get(worldName) != null) {
                for (Claim claim : claimsByWorld.get(worldName)) {
                    int lowestY = -64;
                    int highestY = 320;

                    Color outlineColor = getColorFromName(claim.getOwnerName());
                    Color mainColor = new Color((int) (outlineColor.getRed() * 0.9), (int) (outlineColor.getGreen() * 0.9), (int) (outlineColor.getBlue() * 0.9), 0.2f);

                    Marker claimMarker = ExtrudeMarker.builder()
                            .fillColor(mainColor)
                            .lineColor(outlineColor)
                            .lineWidth(2)
                            .shape(new Shape.Builder()
                                    .addPoint(new Vector2d(claim.getLesserBoundaryCorner().getX(), claim.getLesserBoundaryCorner().getZ()))
                                    .addPoint(new Vector2d(claim.getLesserBoundaryCorner().getX(), claim.getGreaterBoundaryCorner().getZ() + 1))
                                    .addPoint(new Vector2d(claim.getGreaterBoundaryCorner().getX() + 1, claim.getGreaterBoundaryCorner().getZ() + 1))
                                    .addPoint(new Vector2d(claim.getGreaterBoundaryCorner().getX() + 1, claim.getLesserBoundaryCorner().getZ()))
                                    .build(), (float) lowestY, (float) highestY)
                            .label(claim.getOwnerName() + "'s claim")
                            .build();


                    claimsMarkerSet.put(claim.getID().toString(), claimMarker);
                }
            }

            for (BlueMapMap map : maps) {
                map.getMarkerSets().put("claims", claimsMarkerSet);
            }

        }
    }

    private Color getColorFromName(String name) {
        Random random = new Random(name.hashCode());
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 1);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
