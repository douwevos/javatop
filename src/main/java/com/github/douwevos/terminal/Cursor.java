package com.github.douwevos.terminal;

public class Cursor {

	public final int column;
	public final int row;
	
	public Cursor(int column, int row) {
		this.column = column;
		this.row = row;
	}
	
	public Cursor move(int deltaColumn, int deltaRow) {
		return new Cursor(column+deltaColumn, row+deltaRow);
	}

	@Override
	public int hashCode() {
		return column + 683*row;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof Cursor) {
			Cursor that = (Cursor) obj;
			return that.column==column && that.row==row;
		}
		return false;
	}
}
