package com.github.douwevos.terminal.component;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.UpdateContext;

public class Splitter extends FaceContainer {

	private final Direction direction;
	private final int percentile;
	private final boolean drawSplitLines;
	private int lastSplitLocation = -1;
	 

	public Splitter(Face faceA, Face faceB, Direction direction, int percentile) {
		this(faceA, faceB, direction, percentile, false);
	}

	
	public Splitter(Face faceA, Face faceB, Direction direction, int percentile, boolean drawSplitLines) {
		children.add(faceA);
		children.add(faceB);
		this.direction = direction;
		this.percentile = percentile;
		this.drawSplitLines = drawSplitLines;
	}
	
	
	@Override
	public void updateLayout(int x, int y, int width, int height) {
		super.updateLayout(x, y, width, height);
		switch(direction) {
			case HORIZONTAL : splitHorizontal(x, y, width, height); break;
			case VERTICAL : splitVertical(x, y, width, height); break;
		}
	}
	
	@Override
	public int getPreferredHeight() {
		if (direction == Direction.HORIZONTAL) {
			return streamChildren().mapToInt(Face::getPreferredHeight).max().orElse(0);
		}
		return streamChildren().mapToInt(Face::getPreferredHeight).sum();
	}
	
	@Override
	public int getPreferredWidth() {
		if (direction == Direction.VERTICAL) {
			return streamChildren().mapToInt(Face::getPreferredWidth).max().orElse(0);
		}
		return streamChildren().mapToInt(Face::getPreferredWidth).sum();
	}
	
	
	private void splitVertical(int x, int y, int width, int height) {
		Face faceA = children.get(0);
		Face faceB = children.get(1);
		if (percentile<=0) {
			int prefHeight = faceA.getPreferredHeight();
			faceA.updateLayout(x, y, width, prefHeight);
			setSplitLocation(y+prefHeight);
			if (drawSplitLines) {
				prefHeight++;
			}
			faceB.updateLayout(x, y+prefHeight, width, height-prefHeight);
		} 
		else if (percentile>=1000) {
			int prefHeight = faceB.getPreferredHeight();
			setSplitLocation(y+prefHeight);
			if (drawSplitLines) {
				faceA.updateLayout(x, y, width, height-(prefHeight+1));
			} 
			else {
				faceA.updateLayout(x, y, width, height-(prefHeight));
			}
			faceB.updateLayout(x, y+height-prefHeight, width, prefHeight);
		} else {
		
			int d = (height*percentile)/1000;
			int restY = d+1;
			setSplitLocation(restY);
			faceA.updateLayout(x, y, width, restY);
			restY += y;
			if (drawSplitLines) {
				restY++;
			}
			faceB.updateLayout(x, restY, width, y+height-restY);
		}
	}

	private void setSplitLocation(int splitLocation) {
		if (lastSplitLocation == splitLocation) {
			return;
		}
		lastSplitLocation = splitLocation;
		markUpdateFlag(UF_PAINT);
	}


	private void splitHorizontal(int x, int y, int width, int height) {
		Face faceA = children.get(0);
		Face faceB = children.get(1);
		if (percentile<=0) {
			int prefWidth = faceA.getPreferredWidth();
			setSplitLocation(prefWidth);
			faceA.updateLayout(x, y, prefWidth, height);
			if (drawSplitLines) {
				prefWidth++;
			}
			faceB.updateLayout(x+prefWidth, y, width-prefWidth, height);
		} 
		else if (percentile>=1000) {
			int prefWidth = faceB.getPreferredWidth();
			setSplitLocation(prefWidth);
			if (drawSplitLines) {
				faceA.updateLayout(x, y, width-(prefWidth+1), height);
			} else {
				faceA.updateLayout(x, y, width-prefWidth, height);
			}
			faceB.updateLayout(x+width-prefWidth, y, prefWidth, height);
		} else {
			int d = (width*percentile)/1000;
			int restX = d+1;
			setSplitLocation(restX);
			faceA.updateLayout(x, y, restX, height);
			restX += x;
			if (drawSplitLines) {
				restX++;
			}
			faceB.updateLayout(restX, y, x+width-restX, height);
		}
	}
	
	@Override
	public void paint(UpdateContext context) {
		if (drawSplitLines) {
			switch(direction) {
				case HORIZONTAL : drawHorizontalSplitLine(context); break;
				case VERTICAL : drawVerticalSplitLine(context); break;
			}
		}
	}
	
	public AnsiFormat getSplitlineFormat(UpdateContext context) {
		return new AnsiFormat(-1, AnsiFormat.ANSI_GRAY, false);
	}


	private void drawVerticalSplitLine(UpdateContext context) {
		Face faceA = children.get(0);
		int x = getWidth()-1;
		int y = faceA.getHeight();
		AnsiFormat format = getSplitlineFormat(context);
		
		while(x>=0) {
			context.moveCursor(x, y);
			context.writeChar('─', format);
			x--;
		}		
	}


	private void drawHorizontalSplitLine(UpdateContext context) {
		Face faceA = children.get(0);
		int x = faceA.getWidth();
		int y = getHeight()-1;
		
		AnsiFormat format = getSplitlineFormat(context);
		while(y>=0) {
			context.moveCursor(x, y);
			context.writeChar('│', format);
			y--;
		}
	}
	
}
