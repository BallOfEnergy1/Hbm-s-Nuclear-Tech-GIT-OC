package api.hbm.energymk2;

import api.hbm.nodespace.INodeHandler;

public interface IEnergyHandler extends INodeHandler {

    public long getMaxPower();
    public long getPower();
    public void setPower(long power);

    public default long getMaxAmount() {
        return getMaxPower();
    }

    public default long getAmount() {
        return getPower();
    }
    public default void setAmount(long power) {
        setPower(power);
    }

}
