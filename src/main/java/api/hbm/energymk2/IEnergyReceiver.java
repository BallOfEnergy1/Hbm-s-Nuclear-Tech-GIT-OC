package api.hbm.energymk2;

import api.hbm.nodespace.INodeReceiver;

/** If it receives energy, use this */
public interface IEnergyReceiver extends IEnergyHandler, INodeReceiver {

	public default long transferPower(long power) {
		if(power + this.getPower() <= this.getMaxPower()) {
			this.setPower(power + this.getPower());
			return 0;
		}
		long capacity = this.getMaxPower() - this.getPower();
		long overshoot = power - capacity;
		this.setPower(this.getMaxPower());
		return overshoot;
	}
}
