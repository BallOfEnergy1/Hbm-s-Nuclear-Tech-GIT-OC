package api.hbm.nodespace;

import api.hbm.nodespace.Net.NetType;
import com.hbm.inventory.fluid.FluidType;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

public interface INodeConnector {


	public default boolean canConnect(ForgeDirection dir, NetType type)  {return true;}

	/** RENDERING ONLY */
	public default boolean canConnect(IBlockAccess world, int x, int y, int z, ForgeDirection dir, NetType type) {return true;}

	/** FLUIDS ONLY */
	public default boolean canConnect(FluidType type, IBlockAccess world, int x, int y, int z, ForgeDirection dir)  {return true;}
}
