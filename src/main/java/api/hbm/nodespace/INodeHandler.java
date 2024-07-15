package api.hbm.nodespace;


import api.hbm.tile.ILoadedTile;
import com.hbm.util.CompatEnergyControl;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

/** DO NOT USE DIRECTLY! This is simply the common ancestor to providers and receivers, because all this behavior has to be excluded from conductors! */
public interface INodeHandler extends INodeConnector, ILoadedTile {

    public long getAmount();
    public void setAmount(long amount);
    public long getMaxAmount();

    public static final boolean particleDebug = false;

    public default Vec3 getDebugParticlePosMK2() {
        TileEntity te = (TileEntity) this;
        Vec3 vec = Vec3.createVectorHelper(te.xCoord + 0.5, te.yCoord + 1, te.zCoord + 0.5);
        return vec;
    }

    public default void provideInfoForECMK2(NBTTagCompound data) {
        data.setLong(CompatEnergyControl.L_ENERGY_HE, this.getAmount());
        data.setLong(CompatEnergyControl.L_CAPACITY_HE, this.getMaxAmount());
    }
}
