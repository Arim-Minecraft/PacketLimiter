package ca.spottedleaf.packetlimiter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;

public final class PacketLimiter extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
    	saveDefaultConfig();
    	FileConfiguration config = getConfig();
        double interval = config.getDouble("interval"); // seconds
        double maxPacketRate = config.getDouble("max-packet-rate"); // packets per second
        String kickCommand = ChatColor.translateAlternateColorCodes('&', config.getString("kick-command"));

        getLogger().info("Packet sampling interval: " + interval + "s");
        getLogger().info("Max packet rate: " + maxPacketRate + "packets/s");

        PacketListener listener = new PacketListener(this, kickCommand, maxPacketRate, interval * 1000.0);
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    @Override
    public void onDisable() {
        ProtocolLibrary.getProtocolManager().removePacketListeners(this);
    }
}