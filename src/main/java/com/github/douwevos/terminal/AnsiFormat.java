package com.github.douwevos.terminal;

public class AnsiFormat {

	public static final int ANSI_BLACK = 0;
	public static final int ANSI_RED = 1;
	public static final int ANSI_GREEN = 2;
	public static final int ANSI_YELLOW = 3;
	public static final int ANSI_BLUE = 4;
	public static final int ANSI_MAGENTA = 5;
	public static final int ANSI_CYAN = 6;
	public static final int ANSI_WHITE = 7;
	public static final int ANSI_GRAY = 8;
	public static final int ANSI_BRIGHT_RED = 9;
	public static final int ANSI_BRIGHT_GREEN = 10;
	public static final int ANSI_BRIGHT_YELLOW = 11;
	public static final int ANSI_BRIGHT_BLUE = 12;
	public static final int ANSI_BRIGHT_MAGENTA = 13;
	public static final int ANSI_BRIGHT_CYAN = 14;
	public static final int ANSI_BRIGHT_WHITE = 15;
	
	private final int background;
	private final int foreground;
	private final boolean bold;
	
	private final String formatText;

	public AnsiFormat(int background, int foreground) {
		this(background, foreground, false);
	}

	public AnsiFormat(int background, int foreground, boolean bold) {
		this.background = background;
		this.foreground = foreground;
		this.bold = bold;
		StringBuilder buf = new StringBuilder();
		buf.append("\033[");
		
		if (bold) {
			buf.append("1;");
		}
		
		if (background==-1) {
			buf.append("49;");
		} 
		else if (background>=0 && background<8) {
			buf.append('4').append(background).append(';');
		} else {
			buf.append("10").append(background-8).append(';');
		}

		if (foreground==-1) {
			buf.append("39m");
		} else if (foreground>=0 && foreground<8) {
			buf.append('3').append(foreground).append('m');
		} else {
			buf.append('9').append(foreground-8).append('m');
		}

		formatText = buf.toString();
	}

	public AnsiFormat reverse() {
		if (foreground==-1 && background==-1) {
			return new AnsiFormat(ANSI_BRIGHT_YELLOW, ANSI_BLACK, bold);
		}
		return new AnsiFormat(foreground, background, bold);
	}

	
	public int getBackground() {
		return background;
	}
	
	public int getForeground() {
		return foreground;
	}
	
	public boolean isBold() {
		return bold;
	}
	
	public String getFormatText() {
		return formatText;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof AnsiFormat) {
			AnsiFormat other = (AnsiFormat) obj;
			return other.background == background
					&& other.foreground == foreground
					&& other.bold == bold;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return background+foreground*13+(bold ? 0 : 119);
	}

}
