package com.hbm.tileentity;

import api.hbm.energymk2.IEnergyReceiver;
import api.hbm.nodespace.Net.NetType;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

//can be used as a soruce too since the core TE handles that anyway
public class TileEntityProxyEnergy extends TileEntityProxyBase implements IEnergyReceiver {
	
	public boolean canUpdate() {
		return false;
	}

	@Override
	public void setPower(long i) {
		
		TileEntity te = getTE();
		
		if(te instanceof IEnergyReceiver) {
			((IEnergyReceiver)te).setPower(i);
		}
	}

	@Override
	public long getPower() {
		
		TileEntity te = getTE();
		
		if(te instanceof IEnergyReceiver) {
			return ((IEnergyReceiver)te).getPower();
		}
		
		return 0;
	}

	@Override
	public long getMaxPower() {
		
		TileEntity te = getTE();
		
		if(te instanceof IEnergyReceiver) {
			return ((IEnergyReceiver)te).getMaxPower();
		}
		
		return 0;
	}

	@Override
	public long transferPower(long power) {
		
		if(getTE() instanceof IEnergyReceiver) {
			return ((IEnergyReceiver)getTE()).transferPower(power);
		}
		
		return 0;
	}

	@Override
	public boolean canConnect(ForgeDirection dir, NetType type) {
		
		TileEntity te = getTE();
		if(te instanceof IEnergyReceiver) {
			return ((IEnergyReceiver)te).canConnect(dir, type); //for some reason two consecutive getTE calls return different things?
		}
		
		return false;
	}
}
