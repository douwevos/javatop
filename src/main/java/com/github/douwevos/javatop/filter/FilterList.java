package com.github.douwevos.javatop.filter;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.javatop.FilterConfig;
import com.github.douwevos.javatop.FilterConfig.FilterEntry;
import com.github.douwevos.javatop.FilterConfig.FilterType;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Dialog;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.KeyEvent.KeyCode;
import com.github.douwevos.terminal.Rectangle;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.UpdateContext;
import com.github.douwevos.terminal.component.CheckBox;
import com.github.douwevos.terminal.component.FaceComponentScrollable;
import com.github.douwevos.terminal.component.Frame;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.Menu;
import com.github.douwevos.terminal.component.Menu.MenuEntry;

public class FilterList extends FaceComponentScrollable {

	private final KeyAction actionEditMode = new KeyAction("<F2>", new CheckBox("edit mode"), this::toggleEditMode);
	private final KeyAction actionBackspace = new KeyAction("<backspace>", new Label("backspace"), this::callbackBackspace);
	private final KeyAction actionDelete = new KeyAction("<delete>", new Label("delete"), this::callbackDelete);
	private final KeyAction actionEnter = new KeyAction("<enter>", new Label("edit/change"), this::callbackEnter);
	private final KeyAction actionPlus = new KeyAction("<+>", new Label("add"), this::callbackAdd);
	private final KeyAction actionInsert = new KeyAction("<insert>", new Label("insert"), this::callbackInsert);

	private final FilterConfig filterConfig = FilterConfig.getInstance();
	
	private final TerminalWindow terminalWindow;
	
	public FilterList(TerminalWindow terminalWindow) {
		this.terminalWindow = terminalWindow;
	}
	
	
	private int field;
	private int cursorX;
	private int cursorY;
	private boolean editMode;
	
	private String editedValue;

	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		KeyCode code = keyEvent.getCode();
		if (code != null) {
			switch(code) {
				case Space :
					break;
				case Escape :
					if (editMode) {
						if (field==2) {
							actionEditMode.fire();
							return true;
						}
					}
					return false;
				default : 
					return false;
			}
		}
		
		if (!editMode) {
			return false;
		}
		
		if (field == 2) {
		
			if (cursorX>=editedValue.length()) {
				editedValue = editedValue + keyEvent.getKey();
			} else {
				editedValue = editedValue.substring(0, cursorX) + keyEvent.getKey() + editedValue.substring(cursorX);
			}
			cursorX++;
			markUpdateFlag(UF_PAINT);
			return true;
		}
		return false;
	}
	
	
	@Override
	protected void callbackRight() {
		if (editMode) {
			if (field==2) {
				if (cursorX>=editedValue.length()) {
					return;
				}
				cursorX++;
				markUpdateFlag(UF_PAINT);
			}
		} else {
			if (field<2) {
				field++;
				markUpdateFlag(UF_PAINT);
			}
		}
		
//		FilterEntry filterEntryAt = filterConfig.getFilterEntryAt(cursorY);
//
//		
//		
//		String line = model.lines.get(cursorY);
//		if (cursorX>=line.length()) {
//			return;
//		}
//		cursorX++;
//		cursorXMark = cursorX;
//		markUpdateFlag(UF_PAINT);
	}

	@Override
	protected void callbackLeft() {
		if (editMode) {
			
			if (cursorX<=0) {
				return;
			}
			cursorX--;
			markUpdateFlag(UF_PAINT);
		} else {
			if (field>0) {
				field--;
				markUpdateFlag(UF_PAINT);
			}
		}
	}
	
	@Override
	protected void callbackUp() {
		if (editMode) {
//		if (cursorY<=0) {
//			return;
//		}
//		cursorY--;
//		cursorX = cursorXMark;
//		String line = model.lines.get(cursorY);
//		if (cursorX>line.length())  {
//			cursorX = line.length();
//		}
//		markUpdateFlag(UF_PAINT);
		} else {
			if (cursorY<=0) {
				return;
			}
			cursorY--;
			markUpdateFlag(UF_PAINT);
		}
	}

	@Override
	protected void callbackDown() {
		if (editMode) {
//		if (cursorY+1>=model.lines.size()) {
//			return;
//		}
//		cursorY++;
//		cursorX = cursorXMark;
//		String line = model.lines.get(cursorY);
//		if (cursorX>line.length())  {
//			cursorX = line.length();
//		}
//		markUpdateFlag(UF_PAINT);
		} else {
			if (cursorY+1>=filterConfig.filterEntryCount()) {
				return;
			}
			cursorY++;
			markUpdateFlag(UF_PAINT);
		}
	}

	private void callbackBackspace() {
		if (!editMode) {
			return;
		}
		if (field==2) {
			if ((cursorX<=0)) {
				return;
			}
			editedValue = editedValue.substring(0, cursorX-1) +editedValue.substring(cursorX);
			cursorX--;
			markUpdateFlag(UF_PAINT);
			
		}
	}

	private void callbackDelete() {
		if (!editMode) {
			if (cursorY<filterConfig.filterEntryCount()) {
				filterConfig.removeFilterEntryAt(cursorY);
				if (cursorY>=filterConfig.filterEntryCount()) {
					cursorY = filterConfig.filterEntryCount()-1;
					if (cursorY<0) {
						cursorY = 0;
					}
				}
				markUpdateFlag(UF_PAINT);
			}
			return;
		}
		if (field == 2) {
			editedValue = editedValue.substring(0, cursorX) +editedValue.substring(cursorX+1);
			markUpdateFlag(UF_PAINT);
		}
	}

	private void callbackAdd() {
		if (editMode) {
			return;
		}
		cursorY = filterConfig.filterEntryCount();
		filterConfig.addFilterEntryAt(cursorY, new FilterEntry(FilterType.PACKAGE_NAME_STARTS_WITH, true, ""));
		field = 2;
		actionEditMode.fire();
	}

	private void callbackInsert() {
		if (editMode) {
			return;
		}
		filterConfig.addFilterEntryAt(cursorY, new FilterEntry(FilterType.PACKAGE_NAME_STARTS_WITH, true, ""));
		field = 2;
		actionEditMode.fire();
	}

	
	private void callbackEnter() {
		actionEditMode.fire();
//		toggleEditMode();
//		if (!editMode) {
//			return;
//		}
//		String line = model.lines.get(cursorY);
//		model.lines.set(cursorY, line.substring(0, cursorX));
//		cursorY++;
//		model.lines.add(cursorY, line.substring(cursorX));
//		cursorX = 0;
//		cursorXMark = cursorX;
//		markUpdateFlag(UF_PAINT);
	}


	private void toggleEditMode() {
		CheckBox checkBox = (CheckBox) (actionEditMode.faceAction);
		editMode = checkBox.isChecked();
		FilterEntry filterEntry = filterConfig.getFilterEntryAt(cursorY);
		if (editMode) {
			if (field==0) {
				FilterEntry newEntry = new FilterEntry(filterEntry.filterType, !filterEntry.inclusive, filterEntry.value);
				filterConfig.setFilterEntryAt(cursorY, newEntry);
				checkBox.toggle();
				editMode = false;
			} else if (field == 1) {
				Menu menu = new Menu() {
					@Override
					public void onItemSelected(int index, MenuEntry entry) {
						super.onItemSelected(index, entry);
						FilterType filterType = FilterType.valueOf(entry.getText());
						FilterEntry newEntry = new FilterEntry(filterType, filterEntry.inclusive, filterEntry.value);
						filterConfig.setFilterEntryAt(cursorY, newEntry);
						terminalWindow.setDialog(null);
						checkBox.toggle();
						editMode = false;
					}
					
					@Override
					public boolean onKeyEvent(KeyEvent keyEvent) {
						KeyCode code = keyEvent.getCode();
						if (code != null) {
							switch(keyEvent.getCode()) {
								case Space :
									keyEvent = new KeyEvent((char) 13); 
									break;
								case CursorRight :
								case CursorLeft :
								case Escape :
									terminalWindow.setDialog(null);
									checkBox.toggle();
									editMode = false;
									return true;
								default :
									break;
							}
						}
						
						return super.onKeyEvent(keyEvent);
					}
				};
				for(FilterType filterType : FilterType.values()) {
					menu.add(new MenuEntry(""+filterType.name()));
				}
				Frame frame = new Frame(menu, "Filter type");
				Dialog dialog = new Dialog(frame);
				Rectangle onScreenRect = getOnScreenRect();
				dialog.updateLayout(onScreenRect.x+2, onScreenRect.y+cursorY+1, dialog.getPreferredWidth(), dialog.getPreferredHeight());
				terminalWindow.setDialog(dialog);
			} else if (field == 2) {
				editedValue = filterConfig.getFilterEntryAt(cursorY).value;
				cursorX = 0;
			}
		} else {
			if (field == 2) {
				FilterEntry newEntry = new FilterEntry(filterEntry.filterType, filterEntry.inclusive, editedValue);
				filterConfig.setFilterEntryAt(cursorY, newEntry);
			}
		}
		markUpdateFlag(UF_PAINT);
	}
	
	AnsiFormat formatInclusive = new AnsiFormat(-1, AnsiFormat.ANSI_GREEN);
	AnsiFormat formatInclusiveFocus = new AnsiFormat(AnsiFormat.ANSI_BRIGHT_MAGENTA, AnsiFormat.ANSI_BLACK);
	AnsiFormat formatInclusiveSelected = new AnsiFormat(AnsiFormat.ANSI_GREEN, AnsiFormat.ANSI_BLACK);

	AnsiFormat formatExclusive = new AnsiFormat(-1, AnsiFormat.ANSI_RED);
	AnsiFormat formatExclusiveFocus = new AnsiFormat(AnsiFormat.ANSI_BRIGHT_MAGENTA, AnsiFormat.ANSI_BLACK);
//	AnsiFormat formatExclusiveFocus = new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_RED);
	AnsiFormat formatExclusiveSelected = new AnsiFormat(AnsiFormat.ANSI_RED, AnsiFormat.ANSI_BLACK);

	public AnsiFormat pickSelectFormat(boolean isSelected, boolean inclusive) {
		boolean hasFocus = hasFocus();
		if (inclusive) {
			if (isSelected) {
				if (hasFocus) {
					return formatInclusiveFocus;
				}
				return formatInclusiveSelected;
			}
			return formatInclusive;
		}
		if (isSelected) {
			if (hasFocus) {
				return formatExclusiveFocus;
			}
			return formatExclusiveSelected;
		}
		return formatExclusive;

	}

	
	@Override
	public void paint(UpdateContext context) {
		int viewY = getViewY();
		int viewRight = getViewX()+getWidth();
		AnsiFormat formatDefault = context.getFormatDefault();
		
		
		
		for(int y=0; y<getHeight(); y++) {
			int row = viewY + y;
			if (row<filterConfig.filterEntryCount()) {
				boolean isSelectedRow = row==cursorY;
				
				FilterEntry filterEntry = filterConfig.getFilterEntryAt(row);
				boolean isInclusive = filterEntry.inclusive;
				
				AnsiFormat format = pickSelectFormat(isSelectedRow && field==0 && !editMode, isInclusive);
//				AnsiFormat format = context.pickSelectFormat(hasFocus(), isSelectedRow && field==0 && !editMode);
				context.moveCursor(0, row);
				context.writeChar(filterEntry.inclusive ? '+' : '-', format);
				
				format = pickSelectFormat(isSelectedRow && field==1 && !editMode, isInclusive);
//				format = context.pickSelectFormat(hasFocus(), isSelectedRow && field==1 && !editMode);
				context.drawString(2, 28, row, filterEntry.filterType.name(), format, true);

				boolean isEditingValue = isSelectedRow && field==2 && editMode;
				
				context.moveCursor(30, row);
//				context.drawString(30, 28, row, filterEntry.filterType.name(), format, true);
				String line = isEditingValue ? editedValue : filterEntry.value;
				
				format = pickSelectFormat(isSelectedRow && field==2 && !editMode, isInclusive);
//				format = context.pickSelectFormat(hasFocus(), isSelectedRow && field==2 && !editMode);
				context.writeString(line, format);
				
				
//				
//				
//				context.writeString(line, format);
////				context.writeChar(0, format);
//
				int textRight = 30+line.length();
				if (textRight<viewRight) {
//					context.clearLine(row, textRight, viewRight, format);
					context.clearLine(row, textRight, viewRight, context.getFormatDefault());
				}
//
				if (isEditingValue) {
					char charAt = cursorX<line.length() ? line.charAt(cursorX) : ' ';
					context.moveCursor(30+cursorX, row);
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
		keyActionCollector.add(actionInsert);
		keyActionCollector.add(actionPlus);
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
