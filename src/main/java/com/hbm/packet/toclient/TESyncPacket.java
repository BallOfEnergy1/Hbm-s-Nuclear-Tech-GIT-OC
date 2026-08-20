package com.hbm.packet.toclient;

import com.hbm.tileentity.IBufPacketReceiver;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;

/**
 * Combined `ByteBuf` with Minecraft's primitive `Packet` for sending via {@link net.minecraft.tileentity.TileEntity#getDescriptionPacket()}.
 * Converting between IMessage and Packet is hard, apparently.
 */
public class TESyncPacket extends Packet implements IMessage {

	int x;
	int y;
	int z;
	IBufPacketReceiver rec;
	ByteBuf buf;

	public TESyncPacket() { }

	public TESyncPacket(int x, int y, int z, IBufPacketReceiver rec) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.rec = rec;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.x = buf.readInt();
		this.y = buf.readInt();
		this.z = buf.readInt();
		this.buf = buf;
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(x);
		buf.writeInt(y);
		buf.writeInt(z);
		this.rec.serialize(buf);
	}
	
	@Override
	public void readPacketData(PacketBuffer buffer) {
		fromBytes(buffer);
	}
	
	@Override
	public void writePacketData(PacketBuffer buffer) {
		toBytes(buffer);
	}
	
	@Override
	public void processPacket(INetHandler handler) {
		if(Minecraft.getMinecraft().theWorld == null)
			return;

		TileEntity te = Minecraft.getMinecraft().theWorld.getTileEntity(this.x, this.y, this.z);

		if (te instanceof IBufPacketReceiver) {
			try {
				((IBufPacketReceiver) te).deserialize(this.buf);
			} finally {
				this.buf.release();
			}
		}
	}

	public static class Handler implements IMessageHandler<TESyncPacket, IMessage> {

		@Override
		public IMessage onMessage(TESyncPacket m, MessageContext ctx) {
			m.processPacket(ctx.getServerHandler());
			return null;
		}
	}
}
