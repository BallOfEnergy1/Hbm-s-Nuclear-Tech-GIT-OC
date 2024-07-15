package api.hbm.nodespace;

// yeah so this is really stupid
// basically just adapted the original power nodespace to accept any/multiple nets.
// adding a new net type can be done with the following steps
/*
1. Add a new net type to `Net.NetType`.
2. Provide special handlers/providers/receivers as seen in `api.hbm.energymk2`
3. Add net creation for that net's conductor type/connector type under `api.hbm.Nodespace.checkNodeConnection()`
4. (Optional) Provide additional attributes to the net via the `netAttributes` variable and the `Net(NetType, Object)` constructor.
 */


// note from gammawave
// if im being honest i really dont feel qualified to do the fluid shit
// this is already a lot for me and despite me trying for days to get the fluid working i just couldnt