package api.hbm.nodespace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import api.hbm.energymk2.IEnergyHandler;
import api.hbm.fluidmk2.IFluidHandler;
import api.hbm.nodespace.Net.NetType;
import com.hbm.main.MainRegistry;
import com.hbm.util.fauxpointtwelve.BlockPos;
import com.hbm.util.fauxpointtwelve.DirPos;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * The "Nodespace" is an intermediate, "ethereal" layer of abstraction that tracks nodes (i.e. cables) even when they are no longer loaded, allowing continued operation even when unloaded
 * @author hbm
 *
 */
public class Nodespace {
	
	/** Contains all "NodeWorld" instances, i.e. lists of nodes existing per world */
	public static HashMap<World, NodeWorld> worlds = new HashMap<>();
	public static Set<Net> nets = new HashSet<>();
	
	public static Node getNode(World world, int x, int y, int z) {
		NodeWorld nodeWorld = worlds.get(world);
		if(nodeWorld != null) return nodeWorld.nodes.get(new BlockPos(x, y, z));
		return null;
	}
	
	public static void createNode(World world, Node node) {
		NodeWorld nodeWorld = worlds.get(world);
		if(nodeWorld == null) {
			nodeWorld = new NodeWorld();
			worlds.put(world, nodeWorld);
		}
		nodeWorld.pushNode(node);
	}
	
	public static void destroyNode(World world, int x, int y, int z) {
		Node node = getNode(world, x, y, z);
		if(node != null) {
			worlds.get(world).popNode(node);
		}
	}
	
	/** Goes over each node and manages connections */
	public static void updateNodespace() {
		
		for(World world : MinecraftServer.getServer().worldServers) {
			NodeWorld nodes = worlds.get(world);

			if(nodes == null)
				continue;
			
			for(Entry<BlockPos, Node> entry : nodes.nodes.entrySet()) {
				Node node = entry.getValue();
				if(!node.hasValidNet() || node.recentlyChanged) {
					checkNodeConnection(world, node);
					node.recentlyChanged = false;
				}
			}
		}
		
		updateNets();
	}
	
	private static void updateNets() {

		for(Net net : nets) net.resetTracker(); //reset has to be done before everything else
		for(Net net : nets) net.transfer();
	}
	
	/** Goes over each connection point of the given node, tries to find neighbor nodes and to join networks with them */
	private static void checkNodeConnection(World world, Node node) {
		
		for(DirPos con : node.connections) {
			
			Node conNode = getNode(world, con.getX(), con.getY(), con.getZ()); // get whatever neighbor node intersects with that connection
			
			if(conNode != null) { // if there is a node at that place
				
				if(conNode.hasValidNet() && conNode.net == node.net) continue; // if the net is valid and both nodes have the same net, skip
				
				if(checkConnection(conNode, con, false)) {
					if(node.net != null && conNode.net != null && node.net.netType != conNode.net.netType) return;
					connectToNode(node, conNode);
				}
			}
		}
		if(node.net == null || !node.net.isValid()) { // initial net creation
			for (BlockPos position : node.positions) {
				TileEntity te = world.getTileEntity(position.getX(), position.getY(), position.getZ());
				if(te instanceof INodeConductor) { // Make the net's type based on the type exposed by the conductor.
					INodeConductor conductor = (INodeConductor) te;
					new Net(conductor.nodeType()).joinLink(node);
					return; // return statement after each!
				}
				if(te instanceof IEnergyHandler) { // Make an energy net.
					new Net(NetType.ENERGY).joinLink(node);
					return;
				}
				if(te instanceof IFluidHandler) { // Make a fluid net.
					new Net(NetType.FLUID).joinLink(node);
					return;
				}
				// New net types can be added below.
			}
		}

		MainRegistry.logger.error("Nodespace net failed to create.");
	}
	
	public static boolean checkConnection(Node connectsTo, DirPos connectFrom, boolean skipSideCheck) {
		
		for(DirPos revCon : connectsTo.connections) {
			if(revCon.getX() - revCon.getDir().offsetX == connectFrom.getX() && revCon.getY() - revCon.getDir().offsetY == connectFrom.getY() && revCon.getZ() - revCon.getDir().offsetZ == connectFrom.getZ() && (revCon.getDir() == connectFrom.getDir().getOpposite() || skipSideCheck)) {
				return true;
			}
		}
		
		return false;
	}
	
	/** Links two nodes with different or potentially no networks */
	private static void connectToNode(Node origin, Node connection) {
		
		if(origin.hasValidNet() && connection.hasValidNet()) { // both nodes have nets, but the nets are different (previous assumption), join networks
			if (origin.net.links.size() > connection.net.links.size()) {
				origin.net.joinNetworks(connection.net);
			} else {
				connection.net.joinNetworks(origin.net);
			}
			} else if(!origin.hasValidNet() && connection.hasValidNet()) { // origin has no net, connection does, have origin join connection's net
			connection.net.joinLink(origin);
		} else if(origin.hasValidNet() && !connection.hasValidNet()) { // ...and vice versa
			origin.net.joinLink(connection);
		}
	}
	
	public static class NodeWorld {

		/** Contains a map showing where each node is, a node is every spot that a cable exists at.
		 * Instead of the old proxy system, things like substation now create multiple nodes at their connection points */
		public HashMap<BlockPos, Node> nodes = new HashMap<>();
		
		/** Adds a node at all its positions to the nodespace */
		public void pushNode(Node node) {
			for(BlockPos pos : node.positions) {
				nodes.put(pos, node);
			}
		}
		
		/** Removes the specified node from all positions from nodespace */
		public void popNode(Node node) {
			if(node.net != null) node.net.destroy();
			for(BlockPos pos : node.positions) {
				nodes.remove(pos);
				node.expired = true;
			}
		}
		
		/** Grabs the node at one position, then removes it from all positions it occupies */
		public void popNode(BlockPos pos) {
			Node node = nodes.get(pos);
			if(node != null) popNode(node);
		}
	}
	
	public static class Node {
		
		public BlockPos[] positions;
		public DirPos[] connections;
		public Net net;
		public boolean expired = false;
		/**
		 * Okay so here's the deal: The code has shit idiot brain fungus. I don't know why. I re-tested every part involved several times.
		 * I don't know why. But for some reason, during neighbor checks, on certain arbitrary fucking places, the joining operation just fails.
		 * Disallowing nodes to create new networks fixed the problem completely, which is hardly surprising since they wouldn't be able to make
		 * a new net anyway and they will re-check neighbors until a net is found, so the solution is tautological in nature. So I tried limiting
		 * creation of new networks. Didn't work. So what's there left to do? Hand out a mark to any node that has changed networks, and let those
		 * recently modified nodes do another re-check. This creates a second layer of redundant operations, and in theory doubles (in practice,
		 * it might be an extra 20% due to break-off section sizes) the amount of CPU time needed for re-building the networks after joining or
		 * breaking, but it seems to allow those parts to connect back to their neighbor nets as they are supposed to. I am not proud of this solution,
		 * this issue shouldn't exist to begin with and I am going fucking insane but it is what it is.
		 */
		public boolean recentlyChanged = true;
		
		public Node(BlockPos... positions) {
			this.positions = positions;
		}
		
		public Node setConnections(DirPos... connections) {
			this.connections = connections;
			return this;
		}
		
		public Node addConnection(DirPos connection) {
			DirPos[] newCons = new DirPos[this.connections.length + 1];
			for(int i = 0; i < this.connections.length; i++) newCons[i] = this.connections[i];
			newCons[newCons.length - 1] = connection;
			this.connections = newCons;
			return this;
		}
		
		public boolean hasValidNet() {
			return this.net != null && this.net.isValid();
		}
		
		public void setNet(Net net) {
			this.net = net;
			this.recentlyChanged = true;
		}
	}
}
