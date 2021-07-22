package com.github.douwevos.terminal.component;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.KeyEvent.KeyCode;
import com.github.douwevos.terminal.UpdateContext;

public class EditableList extends FaceComponentScrollable {

	private final KeyAction actionEditMode = new KeyAction("<F2>", new CheckBox("edit mode"), this::toggleEditMode);
	private final KeyAction actionBackspace = new KeyAction("<backspace>", new Label("backspace"), this::callbackBackspace);
	private final KeyAction actionDelete = new KeyAction("<delete>", new Label("delete"), this::callbackDelete);
	private final KeyAction actionEnter = new KeyAction("<enter>", new Label("enter"), this::callbackEnter);

	private static Model model = new Model();

	public EditableList() {
	}
	
	
	private int cursorX;
	private int cursorXMark;
	private int cursorY;
	private boolean editMode;

	public void setLines(List<String> text) {
		model.setText(text);
		markUpdateFlag(UF_PAINT);
	}

	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		KeyCode code = keyEvent.getCode();
		if (code != null) {
			switch(code) {
				case Space :
					break;
				default : 
					return false;
			}
		}
		
		if (!editMode) {
			return false;
		}
		
		String line = model.lines.get(cursorY);
		if (cursorX>=line.length()) {
			model.lines.set(cursorY, line + keyEvent.getKey());
		} else {
			String lineNew = line.substring(0, cursorX) + keyEvent.getKey() + line.substring(cursorX);
			model.lines.set(cursorY, lineNew);
		}
		cursorX++;
		markUpdateFlag(UF_PAINT);
		return true;
	}
	
	
	@Override
	protected void callbackRight() {
		String line = model.lines.get(cursorY);
		if (cursorX>=line.length()) {
			return;
		}
		cursorX++;
		cursorXMark = cursorX;
		markUpdateFlag(UF_PAINT);
	}

	@Override
	protected void callbackLeft() {
		if (cursorX<=0) {
			return;
		}
		cursorX--;
		cursorXMark = cursorX;
		markUpdateFlag(UF_PAINT);
	}
	
	@Override
	protected void callbackUp() {
		if (cursorY<=0) {
			return;
		}
		cursorY--;
		cursorX = cursorXMark;
		String line = model.lines.get(cursorY);
		if (cursorX>line.length())  {
			cursorX = line.length();
		}
		markUpdateFlag(UF_PAINT);
	}

	@Override
	protected void callbackDown() {
		if (cursorY+1>=model.lines.size()) {
			return;
		}
		cursorY++;
		cursorX = cursorXMark;
		String line = model.lines.get(cursorY);
		if (cursorX>line.length())  {
			cursorX = line.length();
		}
		markUpdateFlag(UF_PAINT);
	}

	private void callbackBackspace() {
		if (!editMode) {
			return;
		}
		if ((cursorX<=0) && (cursorY<=0)) {
			return;
		}

		String line = model.lines.get(cursorY);
		if (cursorX==0) {
			model.lines.remove(cursorY);
			cursorY--;
			String precLine = model.lines.get(cursorY);
			cursorX = precLine.length();
			cursorXMark = cursorX;
			model.lines.set(cursorY, precLine + line);
		}
		else {
			line = line.substring(0, cursorX-1) +line.substring(cursorX);
			model.lines.set(cursorY, line);
			cursorX--;
			cursorXMark = cursorX;
		}
		markUpdateFlag(UF_PAINT);
	}

	private void callbackDelete() {
		if (!editMode) {
			return;
		}
		String line = model.lines.get(cursorY);
		if (cursorX>=line.length()) {
			if (cursorY+1>=model.lines.size()) {
				return;
			}
			String removed = model.lines.remove(cursorY+1);
			model.lines.set(cursorY, line+removed);
			cursorXMark = cursorX;
		}
		else {
			line = line.substring(0, cursorX) +line.substring(cursorX+1);
			model.lines.set(cursorY, line);
			cursorXMark = cursorX;
		}
		markUpdateFlag(UF_PAINT);
	}

	
	private void callbackEnter() {
		if (!editMode) {
			return;
		}
		String line = model.lines.get(cursorY);
		model.lines.set(cursorY, line.substring(0, cursorX));
		cursorY++;
		model.lines.add(cursorY, line.substring(cursorX));
		cursorX = 0;
		cursorXMark = cursorX;
		markUpdateFlag(UF_PAINT);
	}


	private void toggleEditMode() {
		editMode = ((CheckBox) (actionEditMode.faceAction)).isChecked();
		markUpdateFlag(UF_PAINT);
	}
	
	@Override
	public void paint(UpdateContext context) {
		int viewY = getViewY();
		int viewRight = getViewX()+getWidth();
		AnsiFormat formatDefault = context.getFormatDefault();
		for(int y=0; y<getHeight(); y++) {
			int row = viewY + y;
			AnsiFormat format = context.pickSelectFormat(hasFocus(), row==cursorY && !editMode);
			if (row<model.lineCount()) {
				context.moveCursor(0, row);
				String line = model.lineAt(row);
				
				
				context.writeString(line, format);
//				context.writeChar(0, format);

				int textRight = line.length();
				if (textRight<viewRight) {
					context.clearLine(row, textRight, viewRight, format);
				}

				if (row==cursorY && editMode) {
					char charAt = cursorX<line.length() ? line.charAt(cursorX) : ' ';
					context.moveCursor(cursorX, row);
					format = context.pickSelectFormat(hasFocus(), true);
					context.writeChar(charAt, format);
				}

			} else {
				context.clearLine(row, 0, viewRight, formatDefault);
			}
		}
	}

	
	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		keyActionCollector.add(actionEditMode);
		super.enlistKeyActions(keyActionCollector);
		keyActionCollector.add(actionEnter);
		keyActionCollector.add(actionDelete);
		keyActionCollector.add(actionBackspace);
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
		
	}




	
	
}
