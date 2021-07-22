package com.github.douwevos.terminal.component;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.Rectangle;

public abstract class FaceComponentScrollable extends FaceComponent implements Scrollable {

	private final AnsiFormat labelGray = new AnsiFormat(-1, AnsiFormat.ANSI_GRAY, false);
	
	protected final KeyAction actionCursorUp = new KeyAction("<up>", new Label("up", labelGray), this::callbackUp);
	protected final KeyAction actionCursorDown = new KeyAction("<down>", new Label("down", labelGray), this::callbackDown);
	protected final KeyAction actionCursorLeft = new KeyAction("<left>", new Label("left", labelGray), this::callbackLeft);
	protected final KeyAction actionCursorRight = new KeyAction("<right>", new Label("right", labelGray), this::callbackRight);

	protected final KeyAction actionPageUp = new KeyAction("<page-up>", new Label("page up", labelGray), this::callbackPageUp);
	protected final KeyAction actionPageDown = new KeyAction("<page-down>", new Label("page down", labelGray), this::callbackPageDown);

	
	protected int viewX;
	protected int viewY;
	protected int viewHeight;
	protected int viewWidth;
	
	public int getPreferredHeight() {
		return getViewHeight();
	};
	
	@Override
	public int getPreferredWidth() {
		return getViewWidth();
	}
	
	
	public int getViewX() {
		return viewX;
	}
	
	public int getViewY() {
		return viewY;
	}

	protected void setViewX(int viewX) {
		if (viewX<0) {
			viewX = 0;
		}
		if (this.viewX == viewX) {
			return;
		}
		this.viewX = viewX;
		markUpdateFlag(UF_PAINT);
	}

	
	protected void setViewY(int viewY) {
		if (viewY<0) {
			viewY = 0;
		}
		if (this.viewY == viewY) {
			return;
		}
		this.viewY = viewY;
		markUpdateFlag(UF_PAINT);
	}

	@Override
	public int getViewHeight() {
		return viewHeight;
	}
	
	protected void setViewHeight(int newViewHeight) {
		if (newViewHeight<0) {
			newViewHeight = 0;
		}
		if (viewHeight == newViewHeight)  {
			return;
		}
		viewHeight = newViewHeight;
		markUpdateFlag(UF_PAINT);
	}


	@Override
	public int getViewWidth() {
		return viewWidth;
	}
	
	protected void setViewWidth(int newViewWidth) {
		if (newViewWidth<0) {
			newViewWidth = 0;
		}
		if (viewWidth == newViewWidth)  {
			return;
		}
		viewWidth = newViewWidth;
		markUpdateFlag(UF_PAINT);
	}

	
	protected void callbackDown() {
		int viewY = getViewY()+1;
		if (viewY+getHeight() <= getViewHeight()) {
			setViewY(viewY);
		}
	}
	
	protected void callbackUp() {
		int viewY = getViewY()-1;
		if (viewY>=0) {
			setViewY(viewY);
		}
	}

	protected void callbackLeft() {
		int viewX = getViewX()-1;
		if (viewX>=0) {
			setViewX(viewX);
		}
	}

	protected void callbackRight() {
		int viewX = getViewX()+1;
		if (viewX+getWidth() <= getViewWidth()) {
			setViewX(viewX);
		}
	}

	protected void callbackPageUp() {
		int pageHeight = getHeight()-1;
		int viewY = getViewY()-pageHeight;
		if (viewY<0) {
			viewY = 0;
		}
		setViewY(viewY);
	}

	protected void callbackPageDown() {
		int pageHeight = getHeight()-1;
		int viewY = getViewY()+pageHeight;
		if ((viewY+pageHeight)>=getViewHeight()) {
			viewY = getViewHeight()-(pageHeight+1);
		}
		setViewY(viewY);
	}

	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		if (hasFocus()) {
			keyActionCollector.add(actionCursorUp);
			keyActionCollector.add(actionCursorDown);
			keyActionCollector.add(actionCursorLeft);
			keyActionCollector.add(actionCursorRight);
			keyActionCollector.add(actionPageUp);
			keyActionCollector.add(actionPageDown);
		}
	}

	public void ensureRectangleInView(Rectangle itemRectangle) {
		int viewTop = getViewY();
		int viewBottom = viewTop+getHeight();

		int topOfItem = itemRectangle.y;
		int bottomOfItem = itemRectangle.y+itemRectangle.height;
		
		if (bottomOfItem>=viewBottom) {
			viewTop = bottomOfItem-getHeight();
		}
		
		if (topOfItem<viewTop) {
			viewTop = topOfItem;
		}
		setViewY(viewTop);
		
		
		int viewLeft = getViewX();
		int viewRight = viewLeft+getWidth();

		int leftOfItem = itemRectangle.x;
		int rightOfItem = itemRectangle.x+itemRectangle.width;
		
		if (rightOfItem>=viewRight) {
			viewLeft = rightOfItem-getWidth();
		}
		
		if (leftOfItem<viewLeft) {
			viewLeft = leftOfItem;
		}
		setViewX(viewLeft);
		
	}
	
	
}
