package com.github.douwevos.terminal.component;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.UpdateContext;

public class Text extends FaceComponent {

	private int viewX;
	private int viewY;
	
	private TextModel textModel = new TextModel();
	
	public void setText(String text) {
		textModel.setText(text);
		markUpdateFlag(UF_PAINT);
	}
	
	@Override
	public void paint(UpdateContext context) {
		AnsiFormat format = context.getFormatDefault();
		int viewHeight = getHeight();
		for(int lineY=0; lineY<viewHeight; lineY++) {
			context.moveCursor(0, lineY);
			int lineIndex = lineY + viewY;
			String line = null;
			if (lineIndex<textModel.lines.length) {
				line = textModel.lines[lineIndex];
				for(int cidx=viewX; cidx<line.length(); cidx++) {
					char c = line.charAt(cidx);
					context.writeChar(c, format);
				}
			}
			context.clearToRight(format);
		}
	}
	
	
	@Override
	public int getPreferredWidth() {
		return textModel.maxLineSize;
	}
	
	@Override
	public int getPreferredHeight() {
		return textModel.lines.length;
	}
	
	private static class TextModel {
		
		private int maxLineSize;
		private String lines[];
		
		public TextModel() {
			lines = new String[1];
			lines[0] = "";
		}
		
		public void setText(String text) {
			if (text == null) {
				text = "";
			}
			LineNumberReader reader = new LineNumberReader(new StringReader(text));
			List<String> lines = new ArrayList<>();
			maxLineSize = 0;
			try {
				while(true) {
					String line = reader.readLine();
					if (line==null) {
						break;
					}
					if (line.length()>maxLineSize) {
						maxLineSize = line.length();
					}
					lines.add(line);
				}
				this.lines = lines.toArray(new String[lines.size()]);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
