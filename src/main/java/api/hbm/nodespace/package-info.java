package api.hbm.nodespace;

// yeah so this is really stupid
// basically just adapted the original power nodespace to accept any/multiple nets.
// adding a new net type can be done with the following steps
/*
1. Add new net type to the Net.NetType Enum.
2. Define a handler for the net (see: IEnergyHandler/IFluidHandler). Not required; mainly used for converting `getAmount()` and such to more reasonable names such as `getPower()`
3. Define the Provider/Receiver interfaces (see: IEnergyProvider/IEnergyReceiver)
4. Add new enum and the handler to the code in `Nodespace.java` in the function `checkNodeConnection()`.
4. Done! The new net should respond to any machines attempting to connect to it.
 */