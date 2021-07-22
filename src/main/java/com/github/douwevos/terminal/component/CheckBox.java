package com.github.douwevos.terminal.component;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.UpdateContext;

public class CheckBox extends FaceComponent {

	private String text;
	private AnsiFormat format;
//	private char checkOn = '☑';
//	private char checkOff = '☐';
	
	private char checkOn = '✔';
	private char checkOff = ' ';
	
	private boolean checked;
	
	public CheckBox(String text) {
		this(text, null);
	}
	
	public CheckBox(String text, AnsiFormat format) {
		canFocus = false;
		this.text = text;
		this.format = format;
	}
	
	public void setText(String text) {
		this.text = text;
		markUpdateFlag(UF_LAYOUT|UF_PAINT);
	}
	
	public void setFormat(AnsiFormat format) {
		this.format = format;
		markUpdateFlag(UF_PAINT);
	}
	
	public void toggle() {
		checked = !checked;
		markUpdateFlag(UF_PAINT);
	}

	public void setChecked(boolean checked) {
		if (checked == this.checked) {
			return;
		}
		this.checked = checked;
		markUpdateFlag(UF_PAINT);
	}

	public boolean isChecked() {
		return checked;
	}
	
	@Override
	public int getPreferredHeight() {
		return 1;
	}
	
	@Override
	public int getPreferredWidth() {
		return text.length()+1;
	}
	
	@Override
	public void paint(UpdateContext context) {
		AnsiFormat format = this.format==null ? context.getFormatDefault() : this.format;
		context.drawString(0, getWidth(), 0, text, format, true);
		context.moveCursor(text.length(),0);
		context.writeChar(checked ? checkOn : checkOff, new AnsiFormat(format.getBackground(), AnsiFormat.ANSI_BRIGHT_GREEN, false));
	}
}
