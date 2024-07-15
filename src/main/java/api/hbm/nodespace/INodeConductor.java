package api.hbm.nodespace;

import com.hbm.lib.Library;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import api.hbm.nodespace.Nodespace.Node;
import api.hbm.nodespace.Net.NetType;

public interface INodeConductor extends INodeConnector {

    public default Node createNode() {
        TileEntity tile = (TileEntity) this;
        return new Node(new BlockPos(tile.xCoord, tile.yCoord, tile.zCoord)).setConnections(
                new DirPos(tile.xCoord + 1, tile.yCoord, tile.zCoord, Library.POS_X),
                new DirPos(tile.xCoord - 1, tile.yCoord, tile.zCoord, Library.NEG_X),
                new DirPos(tile.xCoord, tile.yCoord + 1, tile.zCoord, Library.POS_Y),
                new DirPos(tile.xCoord, tile.yCoord - 1, tile.zCoord, Library.NEG_Y),
                new DirPos(tile.xCoord, tile.yCoord, tile.zCoord + 1, Library.POS_Z),
                new DirPos(tile.xCoord, tile.yCoord, tile.zCoord - 1, Library.NEG_Z)
        );
    }

    /**
     * Whether the given side can be connected to
     * dir refers to the side of this block, not the connecting block doing the check
     * @param dir
     * @return
     */
    public default boolean canConnect(ForgeDirection dir, NetType type) {
        return dir != ForgeDirection.UNKNOWN;
    }

    public NetType nodeType();
}
