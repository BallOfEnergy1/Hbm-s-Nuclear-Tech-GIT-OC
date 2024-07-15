package api.hbm.fluidmk2;

import api.hbm.nodespace.INodeConductor;
import api.hbm.nodespace.INodeProvider;
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

/** If it sends fluid, use this */
public interface IFluidProviderMK2 extends IFluidHandler, INodeProvider {

	/** Uses up available power, default implementation has no sanity checking, make sure that the requested power is lequal to the current power */
	public default void useFluid(long fluid) {
		this.setFluid(this.getFluid() - fluid);
	}
	
	public default long getProviderSpeed() {
		return this.getMaxAmount();
	}
	
	public default void tryProvide(World world, int x, int y, int z, ForgeDirection dir) {

		TileEntity te = Compat.getTileStandard(world, x, y, z);
		boolean red = false;
		
		if(te instanceof INodeConductor) {
			INodeConductor con = (INodeConductor) te;
			if(con.canConnect(dir.getOpposite(), con.nodeType())) {

				Node node = Nodespace.getNode(world, x, y, z);
				
				if(node != null && node.net != null) {
					node.net.addProvider(this);
					red = true;
				}
			}
		}
		
		if(te instanceof IFluidReceiverMK2 && te != this) {
			IFluidReceiverMK2 rec = (IFluidReceiverMK2) te;
			Node node = Nodespace.getNode(world, x, y, z);
			if(node != null && node.hasValidNet()) {
				if (rec.canConnect(dir.getOpposite(), node.net.netType)) {
					long provides = Math.min(this.getFluid(), this.getProviderSpeed());
					long receives = Math.min(rec.getMaxFluid() - rec.getFluid(), rec.getReceiverSpeed());
					long toTransfer = Math.min(provides, receives);
					toTransfer -= rec.transferFluid(toTransfer);
					this.useFluid(toTransfer);
				}
			}
		}
		
		if(particleDebug) {
			NBTTagCompound data = new NBTTagCompound();
			data.setString("type", "network");
			data.setString("mode", "power");
			double posX = x + 0.5 - dir.offsetX * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posY = y + 0.5 - dir.offsetY * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			double posZ = z + 0.5 - dir.offsetZ * 0.5 + world.rand.nextDouble() * 0.5 - 0.25;
			data.setDouble("mX", dir.offsetX * (red ? 0.025 : 0.1));
			data.setDouble("mY", dir.offsetY * (red ? 0.025 : 0.1));
			data.setDouble("mZ", dir.offsetZ * (red ? 0.025 : 0.1));
			PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, posX, posY, posZ), new TargetPoint(world.provider.dimensionId, posX, posY, posZ, 25));
		}
	}
}
