package com.github.douwevos.terminal;

public class Rectangle {

	public final int x;
	public final int y;
	public final int width;
	public final int height;
	
	public Rectangle(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rectangle intersection(Rectangle other) {
		
		int leftX = x<other.x ? other.x : x;
		int rightX = x+width>other.x+other.width ? other.x+other.width : x+width;
		if (leftX>=rightX) {
			return null;
		}

		int topY = y<other.y ? other.y : y;
		int bottomY = y+height>other.y+other.height ? other.y+other.height : y+height;

		if (topY>=bottomY) {
			return null;
		}

		return new Rectangle(leftX, topY, rightX-leftX, bottomY-topY);
	}

	public boolean intersects(Rectangle other) {
		
		int leftX = x<other.x ? other.x : x;
		int rightX = x+width>other.x+other.width ? other.x+other.width : x+width;
		if (leftX>=rightX) {
			return false;
		}

		int topY = y<other.y ? other.y : y;
		int bottomY = y+height>other.y+other.height ? other.y+other.height : y+height;

		if (topY>=bottomY) {
			return false;
		}

		return true;
	}

	
	public Rectangle union(Rectangle other) {

		int leftX = x<other.x ? x : other.x;
		int rightX = x+width>other.x+other.width ? x+width : other.x+other.width;

		int topY = y<other.y ? y : other.y;
		int bottomY = y+height>other.y+other.height ? y+height : other.y+other.height;

		return new Rectangle(leftX, topY, rightX-leftX, bottomY-topY);

	}

	
	public boolean contains(int column, int row) {
		return column>=x && row>=y
				&& column<x+width && row<y+height;
	}

	
	@Override
	public int hashCode() {
		return x + 683*y + 2957*width + 7919*height;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Rectangle) {
			Rectangle that = (Rectangle) obj;
			return that.x == x && that.y == y
					&& that.width == width && that.height == height;
		}
		return false;
	}


}
