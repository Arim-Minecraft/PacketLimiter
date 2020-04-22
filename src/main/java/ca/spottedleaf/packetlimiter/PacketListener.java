package ca.spottedleaf.packetlimiter;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.injector.server.TemporaryPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketListener extends PacketAdapter implements Listener {

    private final ConcurrentHashMap<UUID, PacketBucket> buckets = new ConcurrentHashMap<>();

    private final String kickCommand;
    private final double maxPacketRate;
    private final double interval;

    public PacketListener(final PacketLimiter plugin, final String kickCommand, final double maxPacketRate, final double interval) {
        super(plugin, ListenerPriority.LOWEST, getPackets(), ListenerOptions.ASYNC, ListenerOptions.INTERCEPT_INPUT_BUFFER);
        // we want to listen at the earliest stage so we can reduce the overhead of packets going through other plugins,
        // as well as other plugin logic being executed for cancelled packets

        this.kickCommand = kickCommand;
        this.maxPacketRate = maxPacketRate;
        this.interval = interval;
    }

    private static List<PacketType> getPackets() {
        List<PacketType> packets = new ArrayList<>();

        for (final PacketType type : PacketType.values()) {
            if (type.isClient() && type.getProtocol() == PacketType.Protocol.PLAY && type.isSupported()) {
                packets.add(type);
            }
        }

        return packets;
    }

    @Override
    public void onPacketReceiving(final PacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();

        if (player == null || player instanceof TemporaryPlayer) {
            return; // we don't have UUID at this stage
        }

        final UUID uuid = player.getUniqueId();

        final PacketBucket bucket = this.buckets.get(uuid);
        if (bucket == null) {
        	return;
        }

        boolean violation = false;
        final int packets;
        
        synchronized (bucket) {
            final PacketBucket currBucket = this.buckets.get(uuid);

            if (currBucket != bucket) { // O_O
                return;
            }

            if (bucket.violatedLimit) {
                event.setCancelled(true);
                return;
            }

            packets = bucket.incrementPackets(1);

            violation = bucket.violatedLimit = bucket.getCurrentPacketRate() > this.maxPacketRate;
        }
        if (violation) {
        	event.setCancelled(true);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                final Player target = Bukkit.getPlayer(uuid);
                if (target == null) {
                    return;
                }

				target.kickPlayer(this.kickCommand.replace("%PLAYER%", target.getName()).replace("%PACKETS%",
						Integer.toString(packets)));
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();

        this.buckets.put(uuid, new PacketBucket(uuid, this.interval, 150));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.buckets.remove(event.getPlayer().getUniqueId());
    }
}