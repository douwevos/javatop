package com.github.douwevos.terminal.component;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.UpdateContext;

public class Label extends FaceComponent {

	private String text;
	private AnsiFormat format;

	public Label(String text) {
		this(text, null);
	}
	
	public Label(String text, AnsiFormat format) {
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
	
	@Override
	public int getPreferredHeight() {
		return 1;
	}
	
	@Override
	public int getPreferredWidth() {
		return text.length();
	}
	
	@Override
	public void paint(UpdateContext context) {
		AnsiFormat format = this.format==null ? context.getFormatDefault() : this.format;
		context.drawString(0, getWidth(), 0, text, format, true);
	}
}
