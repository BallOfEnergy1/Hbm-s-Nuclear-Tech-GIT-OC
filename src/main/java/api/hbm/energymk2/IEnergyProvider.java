package api.hbm.energymk2;

import api.hbm.nodespace.INodeProvider;

/** If it sends energy, use this */
public interface IEnergyProvider extends IEnergyHandler, INodeProvider {

	/** Uses up available power, default implementation has no sanity checking, make sure that the requested power is lequal to the current power */
	public default void usePower(long power) {
		this.setAmount(this.getAmount() - power);
	}
}
