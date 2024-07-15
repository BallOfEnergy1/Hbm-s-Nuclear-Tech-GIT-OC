package com.hbm.tileentity;

import api.hbm.nodespace.INodeConnector;

public class TileEntityProxyConductor extends TileEntityProxyBase implements INodeConnector {

	@Override
	public boolean canUpdate() {
		return false;
	}
}
