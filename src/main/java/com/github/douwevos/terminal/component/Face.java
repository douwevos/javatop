package com.github.douwevos.terminal.component;

import java.util.List;

import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.Rectangle;
import com.github.douwevos.terminal.UpdateContext;

public abstract class Face {
	
	public static int UF_LAYOUT           = 0x0001;
	public static int UF_CLEAR            = 0x0002;
	public static int UF_PAINT            = 0x0004;
	public static int UF_PARENT_CLEARED   = 0x0008;
	public static int UF_ACTION           = 0x0010;

	private int updateFlags = -1;
	private int x;
	private int y;
	private int width;
	private int height;
	
	private boolean hasFocus;
	
	public void updateLayout(int x, int y, int width, int height) {
		clearUpdateFlag(UF_LAYOUT);
		if ((this.x == x) && (this.y == y) && (this.width==width) && (this.height == height)) {
			return;
		}
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		markUpdateFlag(UF_CLEAR);
	}
	
	public void update(UpdateContext context) {
		if (clearUpdateFlag(UF_CLEAR)) {
			clear(context);
			markUpdateFlag(UF_PAINT);
		}
		
		boolean paintFlag = clearUpdateFlag(UF_PAINT);
		boolean clrParFlag = clearUpdateFlag(UF_PARENT_CLEARED);
		if (paintFlag || clrParFlag) {
			paint(context);
		}
	}
	
	protected boolean clearUpdateFlag(int mask) {
		int newUpdateFlags = updateFlags & ~mask;
		if (updateFlags == newUpdateFlags) {
			return false;
		}
		updateFlags = newUpdateFlags;
		return true;
	}
	
	public boolean markUpdateFlag(int mask) {
		int newUpdateFlags = updateFlags | mask;
		if (updateFlags == newUpdateFlags) {
			return false;
		}
		updateFlags = newUpdateFlags;
		return true;
	}
	
	public int getUpdateFlags() {
		return updateFlags;
	}
	
	public int getRecursiveUpdateFlags() {
		return updateFlags;
	}

	public abstract void paint(UpdateContext context);
	
	public void clear(UpdateContext context) {
		int width = getWidth();
		int height = getHeight();
		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {
				context.moveCursor(x, y);
				context.writeChar(' ', null);
			}
		}
		context.moveCursor(0, 0);
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public Rectangle getOnScreenRect() {
		return new Rectangle(x, y, width, height);
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}

	public abstract boolean onKeyEvent(KeyEvent keyEvent);

	public abstract void enlistFocusElements(List<Face> focusElements);

	public void onFocusChanged(boolean gotFocus) {
		hasFocus = gotFocus;
	}

	public abstract int getPreferredHeight();

	public abstract int getPreferredWidth();
	
	public abstract void enlistKeyActions(KeyActionCollector keyActionCollector); 
	
}
