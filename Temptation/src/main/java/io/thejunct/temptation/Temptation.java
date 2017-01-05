/*
 * Copyright (c) 2017 The Junction Network. All Rights Reserved.
 * Created by PantherMan594.
 */

package io.thejunct.temptation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by david on 12/22.
 *
 * @author david
 */
public class Temptation extends JavaPlugin implements Listener {
    private static final Long JAN_FIRST = LocalDate.of(2016, 1, 1).toEpochDay();
    private static final String PREFIX = ChatColor.RED + ChatColor.ITALIC.toString() + ChatColor.BOLD + "T" + ChatColor.BLUE + ChatColor.BOLD + "J " + ChatColor.GRAY + "Temptation " + ChatColor.DARK_GRAY + ChatColor.BOLD + "> " + ChatColor.BLUE;
    private static Location SPAWN;

    private Set<Player> cooldownSet;
    private Map<UUID, Integer> mobDifficulty;
    private Map<UUID, Integer> diffCooldown;
    private Map<UUID, Location> deathLoc;
    private Map<UUID, List<ItemStack>> deathInv;
    private Map<UUID, Location> tpCyclers;

    private ProtocolManager protocolManager;
    private PacketListener hoverListener;

    private RegionManager wg;
    private ProtectedRegion dpZone;

    private File dataFile;
    private YamlConfiguration data;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        cooldownSet = new HashSet<>();
        mobDifficulty = new HashMap<>();
        diffCooldown = new HashMap<>();
        deathLoc = new HashMap<>();
        deathInv = new HashMap<>();
        tpCyclers = new HashMap<>();

        wg = WGBukkit.getPlugin().getRegionManager(getServer().getWorld("world"));
        dpZone = wg.getRegion("dpzone");

        SPAWN = new Location(Bukkit.getServer().getWorld("world"), 2772.5, 66, 6620.5, 36.75f, 8.9f);


        dataFile = new File(getDataFolder(), "data.yml");
        try {
            if (!getDataFolder().exists()) getDataFolder().createNewFile();
            if (!dataFile.exists()) dataFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        for (Player p : getServer().getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            mobDifficulty.put(uuid, data.getInt(uuid.toString() + ".diff", 30));
            diffCooldown.put(uuid, data.getInt(uuid.toString() + ".cool", 0));
        }

        final String pattern = "((§c§l§oT§9§lJ |§8\\[)(§[0-9a-zA-Z]){1,2}[a-zA-Z]+)\\{\\\\hovName:([§0-9a-zA-Z -_]+)\\\\}(§8]| §8\\|)";
        final Pattern hoverName = Pattern.compile(pattern);

        protocolManager = ProtocolLibrary.getProtocolManager();

        hoverListener = new PacketAdapter(this, ListenerPriority.HIGHEST, PacketType.Play.Server.CHAT, PacketType.Play.Server.TITLE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPacketType() == PacketType.Play.Server.TITLE || event.getPacketType() == PacketType.Play.Server.CHAT) {
                    PacketContainer packet = event.getPacket();
                    if (packet == null || packet.getChatComponents() == null || packet.getChatComponents().read(0) == null)
                        return;
                    final String initialJsonS = packet.getChatComponents().read(0).getJson();
                    TextComponent text = new TextComponent(ComponentSerializer.parse(initialJsonS));
                    List<BaseComponent> newExtras = new ArrayList<>();
                    for (BaseComponent extra : text.getExtra()) {
                        String msg = extra.toLegacyText();
                        if (msg.contains("{\\hovName:")) {
                            Matcher m = hoverName.matcher(msg);
                            int start = 0;

                            while (m.find()) {
                                if (m.start() > start) {
                                    TextComponent partExtra = new TextComponent(TextComponent.fromLegacyText(msg.substring(start, m.start())));
                                    newExtras.add(partExtra);
                                }
                                start = m.end();

                                String stripped = m.group(1) + m.group(5);
                                String hover = m.group(4);

                                TextComponent strippedComponent = new TextComponent(TextComponent.fromLegacyText(stripped));
                                strippedComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hover)));

                                newExtras.add(strippedComponent);
                            }
                            if (start < msg.length() - 1) {
                                TextComponent partExtra = new TextComponent(TextComponent.fromLegacyText(msg.substring(start)));
                                newExtras.add(partExtra);
                            }
                        } else {
                            newExtras.add(extra);
                        }
                    }
                    text.setExtra(newExtras);
                    packet.getChatComponents().write(0, WrappedChatComponent.fromJson(ComponentSerializer.toString(text)));
                }
            }

            @Override
            public void onPacketReceiving(PacketEvent event) {

            }
        };

        try {
            protocolManager.addPacketListener(hoverListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        for (Player p : getServer().getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            if (mobDifficulty.get(uuid) != 30 || diffCooldown.get(uuid) != 0) {
                data.set(uuid.toString() + ".diff", mobDifficulty.get(uuid));
                data.set(uuid.toString() + ".cool", diffCooldown.get(uuid));
            }
        }
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        protocolManager.removePacketListener(hoverListener);
    }

    private void resetDropper(Block bl) {
        Dropper dropper = (Dropper) bl.getState();
        for (int i = 0; i < 9; i++) {
            ItemStack item = dropper.getInventory().getItem(i);
            if (item != null && item.getAmount() != 64) {
                item.setAmount(64);
                dropper.getInventory().setItem(i, item);
            }
        }
    }

    private void tpCycle(final Player p) {
        int iter = 0;
        for (final Player specPlayer : getServer().getOnlinePlayers()) {
            if (specPlayer.isOnline() && !specPlayer.equals(p) && !specPlayer.getGameMode().equals(GameMode.SPECTATOR)) {
                final int delay = iter++ * 200;
                getServer().getScheduler().runTaskLater(this, () -> {
                    if (tpCyclers.containsKey(p.getUniqueId())) {
                        p.sendTitle("", ChatColor.BLUE + specPlayer.getDisplayName(), 10, 180, 10);
                        p.setSpectatorTarget(null);
                        p.teleport(specPlayer);
                    }
                }, delay);
            }
        }
        final int delay = iter * 200;
        getServer().getScheduler().runTaskLater(this, () -> {
            if (tpCyclers.containsKey(p.getUniqueId())) {
                p.sendMessage(PREFIX + "Want to end the cycle? Run " + ChatColor.GRAY + "/tpcycle " + ChatColor.BLUE + "again.");
                tpCycle(p);
            }
        }, delay);
    }

    private void mCapInterface(Player p, int diff, int values) {
        int currCap = mobDifficulty.get(p.getUniqueId());
        p.sendMessage(PREFIX + "Difficulty (click to select):");
        TextComponent text = new TextComponent("Easy <-");
        text.setColor(net.md_5.bungee.api.ChatColor.BLUE);

        TextComponent div = new TextComponent("-");
        div.setColor(net.md_5.bungee.api.ChatColor.BLUE);

        int num = 10;
        for (int i = 1; i <= values; i++) {
            String format = ChatColor.GRAY.toString() + i;
            if (num == currCap) {
                format = ChatColor.GOLD + "[" + i + ChatColor.GOLD + "]";
            }

            TextComponent selection = new TextComponent(TextComponent.fromLegacyText(format));
            selection.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(ChatColor.BLUE + "Mob cap: " + num)));
            selection.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/mcap " + num));
            text.addExtra(selection);
            text.addExtra(div);

            num += diff;
        }

        text.addExtra(new TextComponent(TextComponent.fromLegacyText(ChatColor.BLUE + "> Hard")));
        p.sendMessage(text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && label.equalsIgnoreCase("mcap")) {
            if (args.length == 0) {
                Player p = (Player) sender;
                if (p.hasPermission("group.firstclass")) {
                    mCapInterface(p, 10, 16);
                } else if (p.hasPermission("group.business")) {
                    mCapInterface(p, 15, 11);
                } else if (p.hasPermission("group.economy")) {
                    mCapInterface(p, 30, 6);
                } else {
                    mCapInterface(p, 50, 4);
                }
                return true;
            } else if (args.length == 1) {
                Player p = (Player) sender;

                int cooldown = (int) (diffCooldown.get(p.getUniqueId()) + JAN_FIRST - LocalDate.now().toEpochDay());
                if (cooldown <= 0 || p.isOp()) {
                    try {
                        int cap = Integer.valueOf(args[0]);

                        int diff = 50;
                        if (p.hasPermission("group.firstclass")) {
                            diff = 10;
                        } else if (p.hasPermission("group.business")) {
                            diff = 15;
                        } else if (p.hasPermission("group.economy")) {
                            diff = 30;
                        }

                        if (cap >= 10 && cap <= 160 && (cap - 10) % diff == 0) {
                            mobDifficulty.put(p.getUniqueId(), cap);
                            data.set(p.getUniqueId().toString(), cap);
                            diffCooldown.put(p.getUniqueId(), (int) (LocalDate.now().toEpochDay() + 7 - JAN_FIRST));
                            cap = ((cap - 10) / diff) + 1;
                            sender.sendMessage(PREFIX + "Difficulty successfully changed to " + ChatColor.GRAY + cap + ChatColor.BLUE + ".");
                            return true;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    String timeStr = ChatColor.GRAY.toString() + cooldown + ChatColor.RED + " more days";
                    if (cooldown == 1) {
                        timeStr = timeStr.substring(0, timeStr.length() - 1);
                    }
                    p.sendMessage(PREFIX + ChatColor.RED + "You must wait " + timeStr + " before you can change your difficulty again!");
                    return false;
                }
            }
            sender.sendMessage(PREFIX + ChatColor.RED + "Invalid usage. To change your difficulty: " + ChatColor.GRAY + "/mcap" + ChatColor.RED + ".");
            return false;
        }
        if (sender.isOp() && label.equalsIgnoreCase("dp")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("on")) {
                    for (int x = dpZone.getMinimumPoint().getBlockX(); x <= dpZone.getMaximumPoint().getBlockX(); x++) {
                        for (int y = dpZone.getMinimumPoint().getBlockY(); y <= dpZone.getMaximumPoint().getBlockY(); y++) {
                            for (int z = dpZone.getMinimumPoint().getBlockZ(); z <= dpZone.getMaximumPoint().getBlockZ(); z++) {
                                Block bl = getServer().getWorld("world").getBlockAt(x, y, z);
                                if (bl.getState() instanceof Dropper) {
                                    resetDropper(bl);
                                } else if (bl.getType().equals(Material.REDSTONE_BLOCK)) {
                                    bl.setType(Material.STONE);
                                }
                            }
                        }
                    }
                    getServer().dispatchCommand(sender, "perm group default set essentials.warps.dropparty true");
                    getServer().broadcastMessage(PREFIX + ChatColor.BOLD + "Drop party is starting! Come to " + ChatColor.GRAY + "/warp dropparty" + ChatColor.BLUE + "!");
                    return true;
                } else if (args[0].equalsIgnoreCase("off")) {
                    for (int x = dpZone.getMinimumPoint().getBlockX(); x <= dpZone.getMaximumPoint().getBlockX(); x++) {
                        for (int y = dpZone.getMinimumPoint().getBlockY(); y <= dpZone.getMaximumPoint().getBlockY(); y++) {
                            for (int z = dpZone.getMinimumPoint().getBlockZ(); z <= dpZone.getMaximumPoint().getBlockZ(); z++) {
                                Block bl = getServer().getWorld("world").getBlockAt(x, y, z);
                                if (bl.getType().equals(Material.STONE)) {
                                    bl.setType(Material.REDSTONE_BLOCK);
                                }
                            }
                        }
                    }
                    for (Player p : getServer().getOnlinePlayers()) {
                        if (wg.getApplicableRegions(p.getLocation()).getRegions().contains(wg.getRegion("dparea"))) {
                            p.teleport(SPAWN);
                        }
                    }
                    getServer().dispatchCommand(sender, "perm group default set essentials.warps.dropparty false");
                    getServer().broadcastMessage(PREFIX + "Drop party is over!");
                    return true;
                }
            }
            sender.sendMessage(ChatColor.RED + "Invalid arguments. Usage: /dp [on|off]");
            return false;
        } else if (label.equalsIgnoreCase("tpcycle") && sender instanceof Player) {
            final Player p = (Player) sender;
            if (tpCyclers.containsKey(p.getUniqueId())) {
                p.sendMessage(PREFIX + "Teleport cycle ended. Teleporting to original location....");
                p.setSpectatorTarget(null);
                p.teleport(tpCyclers.get(p.getUniqueId()));
                tpCyclers.remove(p.getUniqueId());
                return true;
            } else if (p.getGameMode().equals(GameMode.SPECTATOR)) {
                final Location origLoc = p.getLocation();
                tpCyclers.put(p.getUniqueId(), origLoc);
                p.sendMessage(PREFIX + "Teleport cycle began. Run " + ChatColor.GRAY + "/tpcycle " + ChatColor.BLUE + "again to end.");
                tpCycle(p);
                return true;
            } else {
                p.sendMessage(PREFIX + ChatColor.RED + "You must be in spectator mode to do that!");
                return false;
            }
        } else if (sender.isOp() && command.getLabel().equalsIgnoreCase("restoreinv")) {
            if (args.length == 1) {
                Player p = getServer().getPlayer(args[0]);
                if (p != null) {
                    if (deathInv.containsKey(p.getUniqueId()) && !deathInv.get(p.getUniqueId()).isEmpty()) {
                        p.getInventory().addItem(deathInv.get(p.getUniqueId()).toArray(new ItemStack[deathInv.get(p.getUniqueId()).size()]));
                        p.sendMessage(PREFIX + "Your inventory was restored!");
                    }
                    sender.sendMessage(PREFIX + ChatColor.GRAY + p.getDisplayName() + ChatColor.BLUE + "'s inventory was restored.");
                    return true;
                } else {
                    sender.sendMessage(PREFIX + ChatColor.RED + "Player " + ChatColor.GRAY + args[0] + ChatColor.RED + " not found!");
                    return false;
                }
            } else {
                sender.sendMessage(PREFIX + ChatColor.RED + "Usage: " + ChatColor.GRAY + "/restoreinv <name>");
                return false;
            }
        }
        sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to use that command!");
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        mobDifficulty.put(uuid, data.getInt(uuid.toString() + ".diff", 30));
        diffCooldown.put(uuid, data.getInt(uuid.toString() + ".cool", 0));

        if (tpCyclers.containsKey(uuid)) {
            e.getPlayer().teleport(tpCyclers.get(uuid));
            tpCyclers.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerJoinEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (mobDifficulty.get(uuid) != 30 || diffCooldown.get(uuid) != 0) {
            data.set(uuid.toString() + ".diff", mobDifficulty.get(uuid));
            data.set(uuid.toString() + ".cool", diffCooldown.get(uuid));
            try {
                data.save(dataFile);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CHUNK_GEN || e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            List<Entity> nearbyEntities = e.getEntity().getNearbyEntities(128, 128, 128);
            int nearbyPlayers = 10;
            int nearbyMonsters = 0;
            for (Entity entity : nearbyEntities) {
                if (entity instanceof Monster) nearbyMonsters++;
                else if (entity instanceof Player && ((Player) entity).getGameMode() != GameMode.SPECTATOR)
                    nearbyPlayers += mobDifficulty.get(entity.getUniqueId());
            }
            if (nearbyPlayers < nearbyMonsters) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGMSwitch(PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode().equals(GameMode.SPECTATOR)) {
            getServer().getScheduler().runTaskLater(this, () -> e.getPlayer().setPlayerListName(ChatColor.WHITE + e.getPlayer().getName()), 20);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGMSwitchCycle(PlayerGameModeChangeEvent e) {
        if (!e.getNewGameMode().equals(GameMode.SPECTATOR) && tpCyclers.containsKey(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)) {
            final Player p = e.getPlayer();
            for (ProtectedRegion rg : wg.getApplicableRegions(p.getLocation())) {
                if (rg.getId().equals("spawn")) {
                    if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) && !p.hasAchievement(Achievement.NETHER_PORTAL)) {
                        e.setCancelled(true);
                        p.teleport(new Location(getServer().getWorld("world"), 2678.5, 66, 6647.0, 90, 0));
                        if (!cooldownSet.contains(p)) {
                            p.sendMessage(PREFIX + "You are not worthy of using this portal!");
                            cooldownSet.add(p);
                            getServer().getScheduler().runTaskLater(this, () -> cooldownSet.remove(p), 10 * 20);
                        }
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (e.getClickedBlock().getType().equals(Material.STONE_BUTTON) && e.getClickedBlock().getRelative(BlockFace.DOWN).getType().equals(Material.WOOD_DOUBLE_STEP)) {
                for (ProtectedRegion rg : wg.getApplicableRegions(p.getLocation())) {
                    if (rg.getId().equals("spawn")) {
                        getServer().dispatchCommand(getServer().getConsoleSender(), "rtp 100 10000 -p " + p.getName() + " -x 2711 -z 6509 -c 60 -w world");
                        break;
                    }
                }
            }
        }
        if (p.isOp() && p.isGliding() && e.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            if (p.getInventory().getItemInMainHand().getType().equals(Material.FIREWORK)) {
                p.getInventory().setItem(p.getInventory().getHeldItemSlot(), new ItemStack(Material.FIREWORK, 65));
                p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
            } else if (p.getInventory().getItemInOffHand().getType().equals(Material.FIREWORK)) {
                p.getInventory().setItemInOffHand(new ItemStack(Material.FIREWORK, 65));
                p.getInventory().setChestplate(new ItemStack(Material.ELYTRA));
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location l = p.getLocation();
        if (wg.getRegion("endportal").contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
            if (!p.hasAchievement(Achievement.END_PORTAL)) {
                p.teleport(SPAWN);
                if (!cooldownSet.contains(p)) {
                    p.sendMessage(PREFIX + "You are not worthy of using this portal!");
                    cooldownSet.add(p);
                    getServer().getScheduler().runTaskLater(this, () -> cooldownSet.remove(p), 10 * 20);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDispense(BlockDispenseEvent e) {
        Block bl = e.getBlock();
        Location loc = bl.getLocation();
        if (dpZone.contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()) && bl.getState() instanceof Dropper) {
            resetDropper(e.getBlock());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathLowest(PlayerDeathEvent e) {
        deathInv.put(e.getEntity().getUniqueId(), e.getDrops());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Location deathLoc = e.getEntity().getLocation();
        this.deathLoc.put(e.getEntity().getUniqueId(), deathLoc);
        getServer().getScheduler().runTaskLater(this, () -> e.getEntity().spigot().respawn(), 20);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (deathLoc.containsKey(uuid)) {
            Location deathLoc = this.deathLoc.get(uuid);
            e.getPlayer().sendMessage(String.format(PREFIX + "Last death location: x: " + ChatColor.GRAY + "%s" + ChatColor.BLUE + ", y: " + ChatColor.GRAY + "%s" + ChatColor.BLUE + ", z: " + ChatColor.GRAY + "%s" + ChatColor.BLUE + ".", deathLoc.getBlockX(), deathLoc.getBlockY(), deathLoc.getBlockZ()));
            this.deathLoc.remove(uuid);
        }
    }
}
