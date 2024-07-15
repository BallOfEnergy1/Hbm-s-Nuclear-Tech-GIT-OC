package com.hbm.tileentity.network;

import api.hbm.nodespace.INodeConductor;
import api.hbm.nodespace.Net.NetType;
import api.hbm.nodespace.Nodespace;
import api.hbm.nodespace.Nodespace.Node;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

public class TileEntityCableBaseNT extends TileEntity implements INodeConductor {
	
	protected Node node;

	@Override
	public void updateEntity() {
		
		if(!worldObj.isRemote) {
			
			if(this.node == null || this.node.expired) {
				
				if(this.shouldCreateNode()) {
					this.node = Nodespace.getNode(worldObj, xCoord, yCoord, zCoord);
					
					if(this.node == null || this.node.expired) {
						this.node = this.createNode();
						Nodespace.createNode(worldObj, this.node);
					}
				}
			}
		}
	}
	
	public boolean shouldCreateNode() {
		return true;
	}
	
	public void onNodeDestroyedCallback() {
		this.node = null;
	}

	@Override
	public void invalidate() {
		super.invalidate();
		
		if(!worldObj.isRemote) {
			if(this.node != null) {
				Nodespace.destroyNode(worldObj, xCoord, yCoord, zCoord);
			}
		}
	}

	@Override
	public boolean canConnect(ForgeDirection dir, NetType type) {
		return dir != ForgeDirection.UNKNOWN && type == NetType.ENERGY;
	}

	@Override
	public NetType nodeType() {
		return NetType.ENERGY;
	}
}
