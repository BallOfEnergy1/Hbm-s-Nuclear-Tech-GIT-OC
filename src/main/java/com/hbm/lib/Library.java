package com.hbm.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import api.hbm.nodespace.INodeConnector;
import api.hbm.nodespace.Net.NetType;
import api.hbm.nodespace.Nodespace;
import api.hbm.nodespace.Nodespace.Node;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.projectile.EntityChopperMine;
import com.hbm.interfaces.IFluidAcceptor;
import com.hbm.interfaces.IFluidDuct;
import com.hbm.interfaces.IFluidSource;
import com.hbm.interfaces.Spaghetti;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModItems;
import com.hbm.tileentity.TileEntityProxyInventory;

import api.hbm.energymk2.IBatteryItem;
import api.hbm.fluid.IFluidConnector;
import api.hbm.fluid.IFluidConnectorBlock;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

@Spaghetti("this whole class")
public class Library {
	
	static Random rand = new Random();
	
	public static boolean checkForHeld(EntityPlayer player, Item item) {
		
		if(player.getHeldItem() == null)
			return false;
		
		return player.getHeldItem().getItem() == item;
	}

	public static final ForgeDirection POS_X = ForgeDirection.EAST;
	public static final ForgeDirection NEG_X = ForgeDirection.WEST;
	public static final ForgeDirection POS_Y = ForgeDirection.UP;
	public static final ForgeDirection NEG_Y = ForgeDirection.DOWN;
	public static final ForgeDirection POS_Z = ForgeDirection.SOUTH;
	public static final ForgeDirection NEG_Z = ForgeDirection.NORTH;
	
	/*
	 * Is putting this into this trash can a good idea? No. Do I have a better idea? Not currently.
	 */
	public static boolean canConnect(IBlockAccess world, int x, int y, int z, ForgeDirection dir /* cable's connecting side */, NetType type) {
		
		if(y > 255 || y < 0)
			return false;
		
		Block b = world.getBlock(x, y, z);
		
		if(b instanceof INodeConnector) {
			INodeConnector con = (INodeConnector) b;

			if(con.canConnect(world, x, y, z, dir.getOpposite() /* machine's connecting side */, type))
				return true;
		}
		
		TileEntity te = world.getTileEntity(x, y, z);
		
		if(te instanceof INodeConnector) {
			INodeConnector con = (INodeConnector) te;

			return con.canConnect(dir.getOpposite() /* machine's connecting side */, type);
		}
		
		return false;
	}

	/** dir is the direction along the fluid duct entering the block */
	public static boolean canConnectFluid(IBlockAccess world, int x, int y, int z, ForgeDirection dir /* duct's connecting side */, FluidType type) {
		
		if(y > 255 || y < 0)
			return false;
		
		Block b = world.getBlock(x, y, z);
		
		if(b instanceof INodeConnector) {
			INodeConnector con = (INodeConnector) b;
			
			if(con.canConnect(type, world, x, y, z, dir.getOpposite() /* machine's connecting side */))
				return true;
		}
		
		TileEntity te = world.getTileEntity(x, y, z);
		
		if(te instanceof IFluidConnector) {
			IFluidConnector con = (IFluidConnector) te;
			
			if(con.canConnect(type, dir.getOpposite() /* machine's connecting side */))
				return true;
		}
		
		return false;
	}
	
	public static boolean checkFluidConnectables(World world, int x, int y, int z, FluidType type)
	{
		TileEntity tileentity = world.getTileEntity(x, y, z);
		if(tileentity != null && tileentity instanceof IFluidDuct && ((IFluidDuct)tileentity).getType() == type)
			return true;
		if((tileentity != null && (tileentity instanceof IFluidAcceptor || 
				tileentity instanceof IFluidSource)) || 
				world.getBlock(x, y, z) == ModBlocks.fusion_hatch ||
				world.getBlock(x, y, z) == ModBlocks.dummy_port_compact_launcher ||
				world.getBlock(x, y, z) == ModBlocks.dummy_port_launch_table ||
				world.getBlock(x, y, z) == ModBlocks.rbmk_loader) {
			return true;
		}
		
		if(world.getBlock(x, y, z) == ModBlocks.machine_mining_laser && tileentity instanceof TileEntityProxyInventory)
			return true;
		
		return false;
	}

	public static EntityLivingBase getClosestEntityForChopper(World world, double x, double y, double z, double radius) {
		double d4 = -1.0D;
		EntityLivingBase entityplayer = null;

		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
			if (world.loadedEntityList.get(i) instanceof EntityLivingBase && !(world.loadedEntityList.get(i) instanceof EntityHunterChopper)) {
				EntityLivingBase entityplayer1 = (EntityLivingBase) world.loadedEntityList.get(i);

				if (entityplayer1.isEntityAlive() && !(entityplayer1 instanceof EntityPlayer && ((EntityPlayer)entityplayer1).capabilities.disableDamage)) {
					double d5 = entityplayer1.getDistanceSq(x, y, z);
					double d6 = radius;

					if (entityplayer1.isSneaking()) {
						d6 = radius * 0.800000011920929D;
					}

					if ((radius < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
						d4 = d5;
						entityplayer = entityplayer1;
					}
				}
			}
		}

		return entityplayer;
	}

	public static EntityPlayer getClosestPlayerForSound(World world, double x, double y, double z, double radius) {
		double d4 = -1.0D;
		EntityPlayer entity = null;

		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
				Entity entityplayer1 = (Entity)world.loadedEntityList.get(i);

				if (entityplayer1.isEntityAlive() && entityplayer1 instanceof EntityPlayer) {
					double d5 = entityplayer1.getDistanceSq(x, y, z);
					double d6 = radius;

					if ((radius < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
						d4 = d5;
						entity = (EntityPlayer)entityplayer1;
					}
			}
		}

		return entity;
	}

	public static EntityHunterChopper getClosestChopperForSound(World world, double x, double y, double z, double radius) {
		double d4 = -1.0D;
		EntityHunterChopper entity = null;

		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
				Entity entityplayer1 = (Entity)world.loadedEntityList.get(i);

				if (entityplayer1.isEntityAlive() && entityplayer1 instanceof EntityHunterChopper) {
					double d5 = entityplayer1.getDistanceSq(x, y, z);
					double d6 = radius;

					if ((radius < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
						d4 = d5;
						entity = (EntityHunterChopper)entityplayer1;
					}
			}
		}

		return entity;
	}

	public static EntityChopperMine getClosestMineForSound(World world, double x, double y, double z, double radius) {
		double d4 = -1.0D;
		EntityChopperMine entity = null;

		for (int i = 0; i < world.loadedEntityList.size(); ++i) {
				Entity entityplayer1 = (Entity)world.loadedEntityList.get(i);

				if (entityplayer1.isEntityAlive() && entityplayer1 instanceof EntityChopperMine) {
					double d5 = entityplayer1.getDistanceSq(x, y, z);
					double d6 = radius;

					if ((radius < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
						d4 = d5;
						entity = (EntityChopperMine)entityplayer1;
					}
			}
		}

		return entity;
	}
	
	public static MovingObjectPosition rayTrace(EntityPlayer player, double length, float interpolation) {
		Vec3 vec3 = getPosition(interpolation, player);
		vec3.yCoord += player.eyeHeight;
		Vec3 vec31 = player.getLook(interpolation);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * length, vec31.yCoord * length, vec31.zCoord * length);
		return player.worldObj.func_147447_a(vec3, vec32, false, false, true);
	}

	public static MovingObjectPosition rayTrace(EntityPlayer player, double length, float interpolation, boolean allowLiquids, boolean disallowNonCollidingBlocks, boolean mopOnMiss) {
		Vec3 vec3 = getPosition(interpolation, player);
		vec3.yCoord += player.eyeHeight;
		Vec3 vec31 = player.getLook(interpolation);
		Vec3 vec32 = vec3.addVector(vec31.xCoord * length, vec31.yCoord * length, vec31.zCoord * length);
		return player.worldObj.func_147447_a(vec3, vec32, allowLiquids, disallowNonCollidingBlocks, mopOnMiss);
	}
	
	public static Vec3 getPosition(float interpolation, EntityPlayer player) {
		if(interpolation == 1.0F) {
			return Vec3.createVectorHelper(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);
		} else {
			double d0 = player.prevPosX + (player.posX - player.prevPosX) * interpolation;
			double d1 = player.prevPosY + (player.posY - player.prevPosY) * interpolation + (player.getEyeHeight() - player.getDefaultEyeHeight());
			double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * interpolation;
			return Vec3.createVectorHelper(d0, d1, d2);
		}
	}
	
	public static List<int[]> getBlockPosInPath(int x, int y, int z, int length, Vec3 vec0) {
		List<int[]> list = new ArrayList<int[]>();
		
		for(int i = 0; i <= length; i++) {
			list.add(new int[] { (int)(x + (vec0.xCoord * i)), y, (int)(z + (vec0.zCoord * i)), i });
		}
		
		return list;
	}
	
	//not great either but certainly better
	public static long chargeItemsFromTE(ItemStack[] slots, int index, long power, long maxPower) {
		
		if(power < 0)
			return 0;
		
		if(power > maxPower)
			return maxPower;

		if(slots[index] != null && slots[index].getItem() instanceof IBatteryItem) {
			
			IBatteryItem battery = (IBatteryItem) slots[index].getItem();

			long batMax = battery.getMaxCharge();
			long batCharge = battery.getCharge(slots[index]);
			long batRate = battery.getChargeRate();
			long toCharge = Math.min(Math.min(power, batRate), batMax - batCharge);
			
			power -= toCharge;
			
			battery.chargeBattery(slots[index], toCharge);
		}
		
		return power;
	}
	
	public static long chargeTEFromItems(ItemStack[] slots, int index, long power, long maxPower) {

		if(slots[index] != null && slots[index].getItem() == ModItems.battery_creative) {
			return maxPower;
		}

		if(slots[index] != null && slots[index].getItem() == ModItems.fusion_core_infinite) {
			return maxPower;
		}

		if(slots[index] != null && slots[index].getItem() instanceof IBatteryItem) {

			IBatteryItem battery = (IBatteryItem) slots[index].getItem();

			long batCharge = battery.getCharge(slots[index]);
			long batRate = battery.getDischargeRate();
			long toDischarge = Math.min(Math.min((maxPower - power), batRate), batCharge);

			battery.dischargeBattery(slots[index], toDischarge);
			power += toDischarge;
		}

		return power;
	}
	
	//Flut-Füll gesteuerter Energieübertragungsalgorithmus
	//Flood fill controlled energy transmission algorithm
	public static void ffgeua(int x, int y, int z, boolean newTact, Object that, World worldObj) {
		
		/*
		 * This here smoldering crater is all that remains from the old energy system.
		 * In loving memory, 2016-2021.
		 * You won't be missed.
		 */
	}
	
	public static void transmitFluid(int x, int y, int z, boolean newTact, IFluidSource that, World worldObj, FluidType type) { }
	
	public static boolean isArrayEmpty(Object[] array) {
		if(array == null)
			return true;
		if(array.length == 0)
			return true;
		
		boolean flag = true;
		
		for(int i = 0; i < array.length; i++) {
			if(array[i] != null)
				flag = false;
		}
		
		return flag;
	}

	// Added for sake of doors
	// Original: Drillgon200: https://thebookofshaders.com/glossary/?search=smoothstep
	public static double smoothstep(double t, double edge0, double edge1){
		t = MathHelper.clamp_double((t - edge0) / (edge1 - edge0), 0.0, 1.0);
		return t * t * (3.0 - 2.0 * t);
	}
	public static float smoothstep(float t, float edge0, float edge1){
		t = MathHelper.clamp_float((t - edge0) / (edge1 - edge0), 0.0F, 1.0F);
		return t * t * (3.0F - 2.0F * t);
	}

	public static boolean isObstructed(World world, double x, double y, double z, double a, double b, double c) {
		MovingObjectPosition pos = world.rayTraceBlocks(Vec3.createVectorHelper(x, y, z), Vec3.createVectorHelper(a, b, c));
		return pos != null;
	}
	
	public static boolean isObstructedOpaque(World world, double x, double y, double z, double a, double b, double c) {
		MovingObjectPosition pos = world.func_147447_a(Vec3.createVectorHelper(x, y, z), Vec3.createVectorHelper(a, b, c), false, true, false);
		return pos != null;
	}
	
	public static int getFirstNullIndex(int start, Object[] array) {
		for(int i = start; i < array.length; i++) {
			if(array[i] == null)
				return i;
		}
		return -1;
	}
	
	public static Block getRandomConcrete() {
		int i = rand.nextInt(20);

		if(i <= 1)
			return ModBlocks.brick_concrete_broken;
		if(i <= 4)
			return ModBlocks.brick_concrete_cracked;
		if(i <= 10)
			return ModBlocks.brick_concrete_mossy;
		
		return ModBlocks.brick_concrete;
	}
}
