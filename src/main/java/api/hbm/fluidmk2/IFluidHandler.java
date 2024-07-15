package api.hbm.fluidmk2;

import api.hbm.nodespace.INodeHandler;

public interface IFluidHandler extends INodeHandler {

    public long getMaxFluid();
    public long getFluid();
    public void setFluid(long power);

    public default long getMaxAmount() {
        return getMaxFluid();
    }

    public default long getAmount() {
        return getFluid();
    }
    public default void setAmount(long power) {
        setFluid(power);
    }

}
