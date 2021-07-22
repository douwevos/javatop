package com.github.douwevos.terminal;

public class TerminalLine {

	char characters[];
	AnsiFormat format[];
	
	private TerminalLine(int columnCount) {
		characters = new char[columnCount];
		format = new AnsiFormat[columnCount];
	}
	
	public TerminalLine(int columnCount, AnsiFormat baseFormat) {
		characters = new char[columnCount];
		format = new AnsiFormat[columnCount];
		for(int idx=0; idx<columnCount; idx++) {
			characters[idx] = ' ';
			format[idx] = baseFormat;
		}
	}
	
	
	public int columnCount() {
		return characters.length;
	}
	
	public char charAt(int column) {
		return characters[column];
	}

	public AnsiFormat formatAt(int column) {
		return format[column];
	}


	public void setCharAt(char ch, AnsiFormat format, int column) {
		if (ch<32) {
			ch='.';
		}
		characters[column] = ch;
		this.format[column] = format;
	}

	public void setFormatAt(AnsiFormat format, int column) {
		this.format[column] = format;
	}


	public TerminalLine copy() {
		TerminalLine copy = new TerminalLine(characters.length);
		System.arraycopy(characters, 0, copy.characters, 0, characters.length);
		System.arraycopy(format, 0, copy.format, 0, format.length);
		return copy;
	}
}
