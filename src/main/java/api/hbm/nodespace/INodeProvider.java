package api.hbm.nodespace;

import com.hbm.packet.AuxParticlePacketNT;
import com.hbm.packet.PacketDispatcher;
import com.hbm.util.Compat;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import api.hbm.nodespace.Nodespace.Node;

public interface INodeProvider extends INodeHandler {

    /** Uses up available power, default implementation has no sanity checking, make sure that the requested power is lequal to the current power */
    public default void use(long amount) {
        this.setAmount(this.getAmount() - amount);
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

                if(node != null && node.hasValidNet()) {
                    node.net.addProvider(this);
                    red = true;
                }
            }
        }

        if(te instanceof INodeReceiver && te != this) {
            INodeReceiver rec = (INodeReceiver) te;
            Node node = Nodespace.getNode(world, x, y, z);
            if(node != null && node.hasValidNet()) {
                if (rec.canConnect(dir.getOpposite(), node.net.netType)) {
                    long provides = Math.min(this.getAmount(), this.getProviderSpeed());
                    long receives = Math.min(rec.getMaxAmount() - rec.getAmount(), rec.getReceiverSpeed());
                    long toTransfer = Math.min(provides, receives);
                    toTransfer -= rec.transfer(toTransfer);
                    this.use(toTransfer);
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
            PacketDispatcher.wrapper.sendToAllAround(new AuxParticlePacketNT(data, posX, posY, posZ), new NetworkRegistry.TargetPoint(world.provider.dimensionId, posX, posY, posZ, 25));
        }
    }
}
