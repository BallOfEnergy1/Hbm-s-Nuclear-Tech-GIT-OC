package com.hbm.packet.toclient;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.Tuple;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BufPacketThreading {

	public static final List<Tuple.Pair<IMessage, TargetPoint>> packetQueue = new ArrayList<>();

	public static ThreadFactory packetThreadFactory = new ThreadFactoryBuilder().setNameFormat("NTM-Packet-Thread-%d").build();

	public static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1,16, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), packetThreadFactory);

	public static void createBufPacket(IMessage message, TargetPoint target) {
		packetQueue.add(new Tuple.Pair<>(message, target));
	}

	public static void onWorldTick() {
		// ENTER MULTITHREADING
		for(Tuple.Pair<IMessage, TargetPoint> packet : packetQueue)
			threadPool.submit(() -> PacketDispatcher.wrapper.sendToAllAround(packet.key, packet.value));
		// EXIT MULTITHREADING
		// ok *now* it should be in the clear, back to single-threading
		// sadly synchronizing threads has an overhead, though from testing, it seems as if for larger servers the benefits massively outweigh the downsides
		synchronized (packetQueue) {
			packetQueue.clear();
		}
	}
}
