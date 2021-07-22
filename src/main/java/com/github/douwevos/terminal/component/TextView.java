package com.github.douwevos.terminal.component;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.UpdateContext;
import com.github.douwevos.terminal.KeyEvent.KeyCode;
import com.github.douwevos.terminal.Rectangle;

public class TextView extends FaceComponentScrollable {

	private final Model model = new Model();
	
	private boolean lineSelectionMode = true;
	private int selectedLine;	
	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		KeyCode code = keyEvent.getCode();
		if (code != null) {
			switch(code) {
				case CursorDown : 
					cursorDown();
					return true;
				
				case CursorUp : 
					cursorUp();
					return true;
					
				case CursorLeft :
					cursorLeft();
					return true;

				case CursorRight :
					cursorRight();
					return true;

				case PgUp :
					pageUp();
					return true;

				case PgDown :
					pageDown();
					return true;
					
				case Home :
					home();
					return true; 

				case End :
					end();
					return true; 

				default :
					break;
			}
		}
		
		return false;
	}
	
	private void cursorDown() {
		if (lineSelectionMode) {
			setSelectedLine(selectedLine+1);
		}
		else {
			int viewY = getViewY()+1;
			if ((viewY+getHeight()-1)<model.lineCount()) {
				setViewY(viewY);
			}
		}
	}
	
	private void cursorUp() {
		if (lineSelectionMode) {
			setSelectedLine(selectedLine-1);
		}
		else {
			int viewY = getViewY()-1;
			if (viewY>=0) {
				setViewY(viewY);
			}
		}
	}

	private void cursorLeft() {
		int viewX = getViewX()-1;
		if (viewX>=0) {
			setViewX(viewX);
		}
	}

	private void cursorRight() {
		int viewX = getViewX()+1;
		if (viewX+getWidth() < model.getMaxLineWidth()) {
			setViewX(viewX);
		}
	}

	private void pageUp() {
		int pageHeight = getHeight()-1;
		int viewY = getViewY()-pageHeight;
		if (viewY<0) {
			viewY = 0;
		}
		setViewY(viewY);
		if (lineSelectionMode) {
			setSelectedLine(selectedLine-pageHeight);
		}
	}

	private void pageDown() {
		int pageHeight = getHeight()-1;
		int viewY = getViewY()+pageHeight;
		if ((viewY+pageHeight)>=model.lineCount()) {
			viewY = model.lineCount()-pageHeight-1;
		}
		setViewY(viewY);
		if (lineSelectionMode) {
			setSelectedLine(selectedLine+pageHeight);
		}
	}

	private void home() {
		setViewX(0);
	}
	
	private void end() {
		if (lineSelectionMode) {
			String lineAt = model.lineAt(selectedLine);
			setViewX(lineAt.length()-getWidth());
		} else {
			setViewX(model.getMaxLineWidth()-getWidth());
		}
	}


	private void setSelectedLine(int newSelectedLine) {
		if (newSelectedLine<0) {
			newSelectedLine = 0;
		}
		if (newSelectedLine>=model.lineCount()) {
			newSelectedLine = model.lineCount()-1;
		}
		if (selectedLine == newSelectedLine) {
			return;
		}
		markUpdateFlag(UF_PAINT);
		selectedLine = newSelectedLine;
		ensureRectangleInView(new Rectangle(getViewX(), selectedLine, 1, 1));
	}

	public void setText(List<String> lines) {
		model.setText(lines);
		markUpdateFlag(UF_PAINT);
	}


	@Override
	public int getViewHeight() {
		return model.lineCount()+1;
	}
	
	@Override
	public int getViewWidth() {
		return model.getMaxLineWidth();
	}
	
	@Override
	public void paint(UpdateContext context) {
		int viewY = getViewY();
		int viewRight = getViewX()+getWidth();
		AnsiFormat formatDefault = context.getFormatDefault();
		for(int y=0; y<getHeight(); y++) {
			int row = viewY + y;
			if (row<model.lineCount()) {
				context.moveCursor(0, row);
				String line = model.lineAt(row);
				
				
				AnsiFormat format = context.pickSelectFormat(hasFocus(), row==selectedLine && lineSelectionMode);
				context.writeString(line, format);

				int textRight = line.length();
				if (textRight<viewRight) {
					context.clearLine(row, textRight, viewRight, format);
				}
				
			} else {
				context.clearLine(row, 0, viewRight, formatDefault);
			}
		}
	}
	
	
	public static class Model {
		private final List<String> lines = new ArrayList<>();

		private int maxLineWidth;
		
		private void validate() {
			maxLineWidth = 0;
			for(String line : lines) {
				int length = line.length();
				if (length>maxLineWidth) {
					maxLineWidth = length;
				}
			}
		}
		
		public String lineAt(int row) {
			return lines.get(row);
		}

		public int lineCount() {
			return lines.size();
		}
		
		public int getMaxLineWidth() {
			return maxLineWidth;
		}


		public void setText(List<String> lines) {
			this.lines.clear();
			if (lines != null) {
				this.lines.addAll(lines);
			}
			validate();
		}
		
		public void setText(String text) {
			lines.clear();
			try {
				LineNumberReader reader = new LineNumberReader(new StringReader(text));
				while(true) {
					String line = reader.readLine();
					if (line==null) {
						break;
					}
					lines.add(line);
				}
			} catch (IOException e) {
			}
			validate();
		}
	}


}
