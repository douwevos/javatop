package com.github.douwevos.terminal;

import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.Scrollable;

public abstract class UpdateContext {

	protected int cursorX;
	protected int cursorY;
	
	public abstract void writeChar(char ch, AnsiFormat format);
	public abstract void writeString(String text, AnsiFormat format);
	
	public abstract Face getFace();
	public abstract UpdateContext withFace(Face face);

	public abstract AnsiFormat getFormatDefault();
	public abstract AnsiFormat getFormatFocusBorder();
	public abstract AnsiFormat getFormatSelectFocus();
	public abstract AnsiFormat getFormatSelect();
	
	public abstract void clearLine(int y, int left, int right, AnsiFormat format);

	
	public void clearToRight(AnsiFormat format) {
		Face face = getFace();
		int cursorX2 = cursorX;
		
		int xLeftView = 0;
		if (face instanceof Scrollable) {
			Scrollable s = (Scrollable) face;
			xLeftView = s.getViewX();
			if (cursorX<xLeftView) {
				cursorX = xLeftView;
			}
		}
		
		
		int xRightView = xLeftView + face.getWidth();
		while(cursorX<xRightView) {
			writeChar(' ', format);
		}
		cursorX = cursorX2;
	}
	
	public void nextLine(boolean atColumn0) {
		cursorY++;
		if (atColumn0) {
			cursorX = 0;
		}
	}

	public void moveCursor(int newX, int newY) {
		cursorX = newX;
		cursorY = newY;
	}

	public AnsiFormat createFormat(int background, int foreground, boolean bold) {
		return new AnsiFormat(background, foreground, bold);
	}

	public AnsiFormat pickSelectFormat(boolean hasFocus, boolean isSelected) {
		if (isSelected) {
			if (hasFocus) {
				return getFormatSelectFocus();
			}
			return getFormatSelect();
		}
		return getFormatDefault();
	}
	
	public Rectangle clipRectangle(Rectangle rectangle) {
		Rectangle viewRectangle = getViewRectangle();
		return viewRectangle.intersection(rectangle);
	}

	private Rectangle getViewRectangle() {
		Face face = getFace();
		if (face instanceof Scrollable) {
			Scrollable s = (Scrollable) face;
			return new Rectangle(s.getViewX(), s.getViewY(), face.getWidth(), face.getHeight());
		}
		else {
			return new Rectangle(0, 0, face.getWidth(), face.getHeight());
		}
	}

	public void clearRectangle(Rectangle rectangle, AnsiFormat ansiFormat) {
		int left = rectangle.x;
		int right = rectangle.x+rectangle.width-1;
		for(int y=0; y<rectangle.height; y++) {
			clearLine(y+rectangle.y, left, right, ansiFormat);
		}
	}

	public void drawString(int x, int maxWidth, int y, String text, AnsiFormat format, boolean clearEol) {
		moveCursor(x, y);
		if (text.length()>maxWidth) {
			writeString(text.substring(0, maxWidth), format);
			return;
		}
		
		if (text.length() < maxWidth && clearEol) {
			StringBuilder buf = new StringBuilder(maxWidth).append(text);
			while(buf.length()<maxWidth) {
				buf.append(' ');
			}
			text = buf.toString();
		}
		writeString(text, format);
	}


}
