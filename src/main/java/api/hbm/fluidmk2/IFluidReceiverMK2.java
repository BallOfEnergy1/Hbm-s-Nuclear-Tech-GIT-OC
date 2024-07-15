package api.hbm.fluidmk2;

import api.hbm.nodespace.INodeConductor;
import api.hbm.nodespace.INodeReceiver;
import api.hbm.nodespace.Nodespace;
import api.hbm.nodespace.Nodespace.Node;
import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.Compat;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** If it receives fluid, use this */
public interface IFluidReceiverMK2 extends IFluidHandler, INodeReceiver {

	public default long transferFluid(long fluid) {
		if(fluid + this.getFluid() <= this.getMaxFluid()) {
			this.setFluid(fluid + this.getFluid());
			return 0;
		}
		long capacity = this.getMaxAmount() - this.getFluid();
		long overshoot = fluid - capacity;
		this.setFluid(this.getMaxAmount());
		return overshoot;
	}
	
	public default long getReceiverSpeed() {
		return this.getMaxAmount();
	}
	
	public default void trySubscribe(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = Compat.getTileStandard(world, x, y, z);
		boolean red = false;
		
		if(te instanceof INodeConductor) {
			INodeConductor con = (INodeConductor) te;
			if(!con.canConnect(dir.getOpposite(), con.nodeType())) return;
			
			Node node = Nodespace.getNode(world, x, y, z);
			
			if(node != null && node.net != null) {
				node.net.addReceiver(this);
				red = true;
			}
		}
		
		if(particleDebug) {
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "network");
			data.setString("mode", "power");
			double posX = x + 0.5 + dir.offsetX * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posY = y + 0.5 + dir.offsetY * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posZ = z + 0.5 + dir.offsetZ * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			data.setDouble("mX", -dir.offsetX * (red ? 0.025 : 0.1));
			data.setDouble("mY", -dir.offsetY * (red ? 0.025 : 0.1));
			data.setDouble("mZ", -dir.offsetZ * (red ? 0.025 : 0.1));
			PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, posX, posY, posZ), new TargetPoint(world.provider.dimensionId, posX, posY, posZ, 25));
		}
	}
	
	public default void tryUnsubscribe(World world, int x, int y, int z) {

		TileEntity te = world.getTileEntity(x, y, z);
		
		if(te instanceof INodeConductor) {
			INodeConductor con = (INodeConductor) te;
			Node node = con.createNode();
			
			if(node != null && node.net != null) {
				node.net.removeReceiver(this);
			}
		}
	}
}
