package com.hukhong.net;

import com.hukhong.HuKhong;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** May chu rieng: in dia chi IP:cong o cuoi log (sau khi dung san thanh xong) de nguoi choi de nhin. */
@EventBusSubscriber(modid = HuKhong.MODID)
public final class ServerAddressBanner {
    private static final Logger LOG = LogUtils.getLogger();
    private static long printAt = -1;
    private ServerAddressBanner() {}

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        if (e.getServer().isDedicatedServer()) printAt = e.getServer().getTickCount() + 40;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (printAt < 0 || s.getTickCount() < printAt) return;
        printAt = -1;
        int port = s.getPort();
        LOG.info("==================================================");
        LOG.info(" MAY CHU HU KHONG DA SAN SANG - nguoi choi nhap:");
        for (String ip : addresses()) { String[] p = ip.split("  ", 2); LOG.info("   {}:{}  {}", p[0], port, p.length > 1 ? p[1] : ""); }
        LOG.info(" (Cung mang nha: dung IP 192.168.x / 10.x; Radmin VPN: IP 26.x)");
        LOG.info(" Tat may chu: go 'stop'");
        LOG.info("==================================================");
    }

    static List<String> addresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address) out.add(a.getHostAddress() + "  (" + ni.getDisplayName() + ")");
            }
        } catch (Exception ignored) {}
        if (out.isEmpty()) out.add("localhost");
        return out;
    }
}
