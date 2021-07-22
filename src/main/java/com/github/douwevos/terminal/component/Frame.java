package com.github.douwevos.terminal.component;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.UpdateContext;

public class Frame extends FaceContainer {

	private String label;
	
	public Frame(Face child, String label) {
		if (child != null) {
			children.add(child);
		}
		this.label = label;
	}
	
	public void setLabel(String label) {
		this.label = label;
		markUpdateFlag(UF_PAINT);
	}
	
	@Override
	public void addChild(Face face) {
		if (!children.isEmpty()) {
			if (children.get(0) == face) {
				return;
			}
		}
		children.clear();
		super.addChild(face);
	}
	
	@Override
	public void updateLayout(int x, int y, int width, int height) {
		super.updateLayout(x, y, width, height);
		if (children.isEmpty()) {
			return;
		}
		Face child = children.get(0);
		child.updateLayout(x+1, y+1, width-2, height-2);
	}
	
	@Override
	public int getPreferredHeight() {
		if (children.isEmpty()) {
			return 0;
		}
		Face child = children.get(0);
		int preferredHeight = child.getPreferredHeight();
		return preferredHeight==0 ? 0 : preferredHeight+2;
	}

	@Override
	public int getPreferredWidth() {
		if (children.isEmpty()) {
			return 0;
		}
		Face child = children.get(0);
		int preferredWidth = child.getPreferredWidth();
		return preferredWidth==0 ? 0 : preferredWidth+2;
	}

	
	@Override
	public void paint(UpdateContext context) {
		int right = getWidth()-1;
		int bottom = getHeight()-1;
		if ((right<=0) || (bottom<=0)) {
			return;
		}
		
		AnsiFormat format = context.getFormatDefault();
		AnsiFormat formatLabel = format;
		if (children.get(0).hasFocus()) {
			format = context.getFormatFocusBorder();
			formatLabel = 	new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_YELLOW, false);
		}
		
		int lleft = Integer.MAX_VALUE;
		int lright = 0;
		if (label != null) {
			int length = label.length();
			lleft = (right-(length+2))/2;
			if (lleft<1) {
				lleft = 1;
			}
			lright = lleft+length+1;
			if (lright>=right) {
				lright = right-1;
			}
			
			context.moveCursor(lleft, 0);
			context.writeChar('[', formatLabel);
			context.moveCursor(lright, 0);
			context.writeChar(']', formatLabel);
			for(int idx=0; idx<(lright-lleft-1); idx++) {
				context.moveCursor(lleft+1+idx, 0);
				context.writeChar(label.charAt(idx), formatLabel);
			}
		}
		
		for(int x=1; x<right; x++) {
			if (x<lleft || x>lright) {
				context.moveCursor(x, 0);
				context.writeChar('─', format);
			}
			context.moveCursor(x, bottom);
			context.writeChar('─', format);
		}
		
		for(int y=1; y<bottom; y++) {
			context.moveCursor(0, y);
			context.writeChar('│', format);
			context.moveCursor(right, y);
			context.writeChar('│', format);
		}
		
		context.moveCursor(0, 0);
		context.writeChar('┌', format);
		context.moveCursor(right, bottom);
		context.writeChar('┘', format);

		context.moveCursor(0, bottom);
		context.writeChar('└', format);
		context.moveCursor(right, 0);
		context.writeChar('┐', format);
	}
	
}
