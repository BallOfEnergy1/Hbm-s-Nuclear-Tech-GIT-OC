package com.hbm.inventory.gui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import javax.annotation.Nonnegative;

import com.google.common.annotations.Beta;
import com.hbm.lib.Library;
import com.hbm.lib.RefStrings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.Container;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public abstract class GuiInfoContainer extends GuiContainer {
	
	static final ResourceLocation guiUtil =  new ResourceLocation(RefStrings.MODID + ":textures/gui/gui_utility.png");
	protected static final ResourceLocation numDisplays = new ResourceLocation(RefStrings.MODID, "textures/gui/gauges/seven_segment.pn");
	/** Default text color **/
	public static final int color0 = 4210752;
	/** Green computer color **/
	public static final int color1 = 0x00ff00;
	public static final char slimCursor = '\u2502';
	public static final char blockCursor = '\u2588';
	public static final ResourceLocation keyboard = new ResourceLocation(RefStrings.MODID, "misc.keyPress"); 

	public GuiInfoContainer(Container p_i1072_1_) {
		super(p_i1072_1_);
	}
	
	public void drawFluidInfo(String[] text, int x, int y) {
		this.func_146283_a(Arrays.asList(text), x, y);
	}
	
	public void drawElectricityInfo(GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height, long power, long maxPower) {
		if(x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY)
			gui.drawFluidInfo(new String[] { Library.getShortNumber(power) + "/" + Library.getShortNumber(maxPower) + "HE" }, mouseX, mouseY);
	}
	
	public void drawCustomInfo(GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height, String[] text) {
		if(x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY)
			this.func_146283_a(Arrays.asList(text), mouseX, mouseY);
	}
	
	public void drawCustomInfoStat(int mouseX, int mouseY, int x, int y, int width, int height, int tPosX, int tPosY, String[] text) {
		
		if(x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY)
			this.func_146283_a(Arrays.asList(text), tPosX, tPosY);
	}
	
	public void drawInfoPanel(int x, int y, int width, int height, int type) {

		Minecraft.getMinecraft().getTextureManager().bindTexture(guiUtil);
		
		switch(type) {
		case 0:
			//Small blue I
			drawTexturedModalRect(x, y, 0, 0, 8, 8); break;
		case 1:
			//Small green I
			drawTexturedModalRect(x, y, 0, 8, 8, 8); break;
		case 2:
			//Large blue I
			drawTexturedModalRect(x, y, 8, 0, 16, 16); break;
		case 3:
			//Large green I
			drawTexturedModalRect(x, y, 24, 0, 16, 16); break;
		case 4:
			//Small red !
			drawTexturedModalRect(x, y, 0, 16, 8, 8); break;
		case 5:
			//Small yellow !
			drawTexturedModalRect(x, y, 0, 24, 8, 8); break;
		case 6:
			//Large red !
			drawTexturedModalRect(x, y, 8, 16, 16, 16); break;
		case 7:
			//Large yellow !
			drawTexturedModalRect(x, y, 24, 16, 16, 16); break;
		case 8:
			//Small blue *
			drawTexturedModalRect(x, y, 0, 32, 8, 8); break;
		case 9:
			//Small grey *
			drawTexturedModalRect(x, y, 0, 40, 8, 8); break;
		case 10:
			//Large blue *
			drawTexturedModalRect(x, y, 8, 32, 16, 16); break;
		case 11:
			//Large grey *
			drawTexturedModalRect(x, y, 24, 32, 16, 16); break;
		}
	}
	
	/**
	 * Seven segment style displays for GUIs, tried to be as adaptable as possible. Still has some bugs that need to be ironed out but it works for the most part.
	 * @author UFFR
	 *
	 */
	@Beta
	public class NumberDisplay
	{
		/** The display's X coordinate **/
		private int displayX;
		/** The display's Y coordinate **/
		private int displayY;
		/** The X coordinate of the reference **/
		private int referenceX = 10;
		/** The Y coordinate of the reference **/
		private int referenceY = 18;
		/** The amount of padding between digits, default 3 **/
		@Nonnegative
		private byte padding = 3;
		/** Does it blink or not, default false, not yet used **/
		private boolean blink = false;
		/** Max number the display can handle **/
		private float maxNum;
		/** Min number the display can handle **/
		private float minNum;
		private boolean customBounds = false;
		// Should it be a decimal number?
		private boolean isFloat = false;
		// How many trailing zeros?
		private byte floatPad = 1;
		/** Does it pad out with zeros **/
		private boolean pads = false;
		/** Max number of digits the display has, default 3 **/
		@Nonnegative
		private byte digitLength = 3;
		private Number numIn = 0;
		private char[] toDisp = {'0', '0', '0'};
		@Nonnegative
		private short dispOffset = 0;
		/**
		 * Construct a new number display
		 * @param dX X coordinate of the display
		 * @param dY Y coordinate of the display
		 * @param c Color to use, invalid enums will default to yellow
		 */
		public NumberDisplay(int x, int y, EnumChatFormatting c)
		{
			this(x, y);
			setupColor(c);
		}
		/**
		 * Construct a new number display, color is yellow
		 * @param x X coordinate of the display
		 * @param y Y coordinate of the display
		 */
		public NumberDisplay(int x, int y)
		{
			displayX = x;
			displayY = y;
		}
		public void setupColor(EnumChatFormatting c)
		{
			byte row = 4, column = 3;
			switch (c)
			{
			case OBFUSCATED:
			case RESET:
			case STRIKETHROUGH:
			case UNDERLINE:
				break;
			default:
				column = (byte) (c.ordinal() % 4);
				row = (byte) Math.floorDiv(c.ordinal(), 4);
				break;
			}
//			System.out.println(column);
//			System.out.println(row);
			referenceY = 6 * row;
			referenceX = 5 * column;
		}
		/**
		 * Draw custom number
		 * @param num - The char array that has the number
		 */
		public void drawNumber(char[] num)
		{
			if (blink && !Library.getBlink())
				return;
				
			short gap = (short) (digitLength - num.length);
			for (int i = 0; i < num.length; i++)
			{
				if (num[i] == '.')
					gap--;
				dispOffset = (short) ((padding + 6) * (i + gap));
				drawChar(num[i]);
			}
			if (pads)
				padOut(gap);
		}
		/** Draw the previously provided number **/
		public void drawNumber()
		{
//			System.out.println(referenceX);
//			System.out.println(referenceY);
			mc.getTextureManager().bindTexture(numDisplays);

			if (isFloat)
				formatForFloat();
			drawNumber(toDisp);
		}
		public void drawNumber(Number num)
		{
			setNumber(num);
			drawNumber();
		}
		private void padOut(short gap)
		{
			if (gap == 0)
				return;
			for (int i = 0; i < gap; i++)
			{
				dispOffset = (short) ((padding + 6) * i);
				drawChar('0');
			}
		}
		
		/** Draw a single character (requires dispOffset to be set) **/
		public void drawChar(char num)
		{
//			System.out.println(num);
			switch (num)
			{
			case '1':
				drawVertical(1, 0);
				drawVertical(1, 1);
				break;
			case '2':
				drawHorizontal(0);
				drawVertical(1, 0);
				drawHorizontal(1);
				drawVertical(0, 1);
				drawHorizontal(2);
				break;
			case '3':
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(1, 0);
				drawVertical(1, 1);
				break;
			case '4':
				drawVertical(0, 0);
				drawVertical(1, 0);
				drawVertical(1, 1);
				drawHorizontal(1);
				break;
			case '5':
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(1, 1);
				break;
			case '6':
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(0, 1);
				drawVertical(1, 1);
				break;
			case '7':
				drawHorizontal(0);
				drawVertical(1, 0);
				drawVertical(1, 1);
				break;
			case '8':
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(1, 0);
				drawVertical(0, 1);
				drawVertical(1, 1);
				break;
			case '9':
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(1, 0);
				drawVertical(1, 1);
				break;
			case '0':
				drawHorizontal(0);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(0, 1);
				drawVertical(1, 0);
				drawVertical(1, 1);
				break;
			case '-':
				drawHorizontal(1);
				break;
			case '.':
				drawPeriod();
				break;
			case 'E':
			default:
				drawHorizontal(0);
				drawHorizontal(1);
				drawHorizontal(2);
				drawVertical(0, 0);
				drawVertical(0, 1);
				break;
			}
		}
		
		private void drawHorizontal(int pos)
		{
			byte offset = (byte) (pos * 6);
			renderSegment(guiLeft + displayX + dispOffset + 1, guiTop + displayY + offset, referenceX + 1, referenceY, 4, 1);
			//System.out.println(referenceX + 1 + ", " + referenceY + ", " + pos);
		}
		
		private void drawPeriod()
		{
			renderSegment(guiLeft + displayX + dispOffset + padding - (int) Math.ceil(padding / 2) + 5, guiTop + displayY + 12, referenceX + 1, referenceY, 1, 1);
		}
		
		private void drawVertical(int posX, int posY)
		{
			byte offsetX = (byte) (posX * 5);
			byte offsetY = (byte) (posY * 6);
			renderSegment(guiLeft + displayX + offsetX + dispOffset, guiTop + displayY + offsetY + 1, referenceX, referenceY + 1, 1, 5);
		}
		/**
		 * drawTexturedModalRect() for cool kids
		 * @param renX X coordinate to render the part
		 * @param renY Y coordinate to render the part
		 * @param refX X coordinate of the reference
		 * @param refY Y coordinate of the reference
		 * @param width Relevant for horizontals
		 * @param height Relevant for verticals
		 */
		private void renderSegment(int renX, int renY, int refX, int refY, int width, int height)
		{
			final Tessellator tess = Tessellator.instance;
			final float z = GuiInfoContainer.this.zLevel;
			tess.startDrawingQuads();
	        tess.addVertexWithUV(renX, renY + height, z, refX, (refY + height));
	        tess.addVertexWithUV(renX + width, renY + height, z, (refX + width), (refY + height));
	        tess.addVertexWithUV(renX + width, renY + 0, z, (refX + width), refY);
	        tess.addVertexWithUV(renX, renY, z, refX, refY);
			tess.draw();
		}

		public void setNumber(Number num)
		{
			numIn = num;
			if (customBounds)
				numIn = MathHelper.clamp_double(num.doubleValue(), minNum, maxNum);
			if (isFloat)
				formatForFloat();
			else
			{
				toDisp = new Long(Math.round(numIn.doubleValue())).toString().toCharArray();
				toDisp = truncOrExpand();
			}
		}
		/** Get the set number **/
		public Number getNumber()
		{
			return numIn;
		}
		/** Get the char array for display **/
		public char[] getDispNumber()
		{
			return toDisp.clone();
		}
		/** Make the display blink **/
		public NumberDisplay setBlinks()
		{
			blink = true;
			return this;
		}
		/** Padding between digits, default 3 **/
		public NumberDisplay setPadding(@Nonnegative int p)
		{
			padding = (byte) p;
			return this;
		}
		/** Max number of digits **/
		public NumberDisplay setDigitLength(@Nonnegative int l)
		{
			digitLength = (byte) l;
			toDisp = truncOrExpand();
			return this;
		}
		/** Set custom number bounds **/
		public NumberDisplay setMaxMin(float max, float min)
		{
			if (min > max)
				throw new IllegalArgumentException("Minimum value is larger than maximum value!");
			maxNum = max;
			minNum = min;
			customBounds = true;
			return this;
		}
		/** Pad out the left side of the number with zeros **/
		public NumberDisplay setPadNumber()
		{
			pads = true;
			return this;
		}
		/** Set the number to be a decimal, default zero trailing is 1 **/
		public NumberDisplay setFloat()
		{
			return setFloat(1);
		}
		/** Set the number to be a decimal with specified zero trailing **/
		public NumberDisplay setFloat(@Nonnegative int pad)
		{
			floatPad = (byte) pad;
			isFloat = true;
			
			formatForFloat();
			
			return this;
		}
		private void formatForFloat()
		{
			BigDecimal bd = new BigDecimal(numIn.toString());
			bd = bd.setScale(floatPad, RoundingMode.HALF_UP);
			
//			char[] proc = new Double(bd.doubleValue()).toString().toCharArray();
			char[] proc = bd.toString().toCharArray();
			proc = Double.valueOf(Library.roundDecimal(numIn.doubleValue(), floatPad)).toString().toCharArray();
			
			if (proc.length == digitLength)
				toDisp = proc;
			else
				toDisp = truncOrExpand();
		}
		@Beta
		private char[] truncOrExpand()
		{
			if (isFloat)
			{
				char[] out = Arrays.copyOf(toDisp, digitLength);
				for (int i = 0; i < digitLength; i++)
					if (out[i] == '\u0000')
						out[i] = '0';
				return out.clone();
			}
			return toDisp;
		}
	}
}
