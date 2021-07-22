package com.github.douwevos.terminal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyActionListObserver;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.UpdateContext;

public class ButtonBar extends FaceComponent implements KeyActionListObserver {

	private final ButtonModel buttonModel;
	
	private int buttonPadding;
	private int columnPadding;
	
	public ButtonBar(int limit, int buttonPadding, int columnPadding) {
		this.buttonModel = new ButtonModel(limit);
		this.buttonPadding = buttonPadding;
		this.columnPadding = columnPadding;
		canFocus = false;
	}
	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		String keyText = keyEvent.toKeyText();
		for(KeyAction keyAction : buttonModel.actions) {
			if (Objects.equals(keyAction.key, keyText) && keyAction.runnable!=null) {
				keyAction.fire();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		keyActionCollector.add(this);		
	}
	
	@Override
	public void onNewKeyActionList(KeyActionCollector keyActionCollector) {
		buttonModel.actions.clear();
		buttonModel.actions.addAll(keyActionCollector.keyActions);
		buttonModel.isDirty = true;
		markUpdateFlag(UF_LAYOUT|UF_PAINT|UF_CLEAR);
	}
	
	
	public ButtonModel getButtonModel() {
		return buttonModel;
	}

	@Override
	public int getRecursiveUpdateFlags() {
		Stream<Face> stream = buttonModel.actions.stream().filter(s -> s.shown).map(s -> s.faceAction).filter(Objects::nonNull);
		int childFlags = stream.mapToInt(s -> s.getRecursiveUpdateFlags()).reduce((a,b) -> a|b).orElse(0);
		return childFlags | getUpdateFlags();
	}

	
	@Override
	public int getPreferredHeight() {
		buttonModel.validate();
		int result = buttonModel.limit;
		int itemCount = buttonModel.visibileActions;
		if (itemCount<result) {
			result = itemCount;
		}
		return result;
	}
	
	
	@Override
	public int getPreferredWidth() {
		buttonModel.validate();
		if (buttonModel.columns.isEmpty()) {
			return 0;
		}
		int sum = buttonModel.columns.stream().mapToInt(s -> s.buttonSize(buttonPadding)).sum();
		int innerCount = buttonModel.columns.size()-1;
		if (innerCount>0) {
			sum += innerCount*columnPadding;
		}
		return sum;
	}
	
	@Override
	public void update(UpdateContext context) {
		super.update(context);
		for(KeyAction action : buttonModel.actions) {
			Face child = action.faceAction;
			if (child == null) {
				continue;
			}
			int childUpdateFlags = child.getRecursiveUpdateFlags();
			if ((childUpdateFlags & (UF_CLEAR|UF_PAINT)) != 0) {
				UpdateContext contextSub = context.withFace(child);
				child.update(contextSub);
			}
		}
		
	}

	
	@Override
	public void paint(UpdateContext context) {
		buttonModel.validate();
		List<ButtonColumn> columns = buttonModel.columns;

		int cursorX = 0;
		int cursorY = 0;
		
		ButtonColumn buttonColumn = null;
		boolean columnHasShift = false;
		int usePaddingAtEnd = 0;
		int listIndex = 0;
		
		List<KeyAction> actions = buttonModel.actions;

		columnHasShift = false;
		for(int idx=0; idx<actions.size(); idx++) {
			KeyAction action = actions.get(idx);
			if (!action.shown) {
				continue;
			}
			if ((listIndex%buttonModel.limit) == 0) {
				if (buttonColumn != null) {
					cursorY = 0;
					cursorX += buttonColumn.buttonSize(buttonPadding);
					cursorX += usePaddingAtEnd;
				}
				int column = idx/buttonModel.limit;
				buttonColumn = columns.get(column);
				usePaddingAtEnd = columnPadding;
				if (column >= columns.size()) {
					usePaddingAtEnd = 0;
				}
//				columnHasShift = buttonColumn.hasShift;
			}
			listIndex++;
			
			int startX = cursorX;
			int endX = startX+buttonColumn.keySize;
			writeLabel(context, cursorY, startX, endX, action.key);
			
			AnsiFormat defaultFormat = context.getFormatDefault();
			for(int r=buttonPadding; r>0; r--) {
				context.writeChar(' ', defaultFormat);
			}

			startX = cursorX + buttonColumn.keySize + buttonPadding;
			endX = startX+buttonColumn.textSize;
			
			if (action.faceAction != null) {
				Face label = action.faceAction;
				int shift = 0;
				if (columnHasShift && !(label instanceof CheckBox)) {
					shift = 1;
				}
				
				label.updateLayout(startX+shift, cursorY, endX-(startX+shift), 1);
				UpdateContext withFace = context.withFace(label);
				label.paint(withFace);
			}
			
			cursorY++;
		}
		
	}



	private void writeLabel(UpdateContext context, int cursorY, int startX, int endX, String key) {
		AnsiFormat formatBrakes = new AnsiFormat(-1, AnsiFormat.ANSI_GRAY, false); 
		AnsiFormat formatLab = new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_YELLOW, false); 
		context.moveCursor(startX, cursorY);
		for(int idx=0; startX<endX; idx++, startX++) {
			AnsiFormat format = formatLab;
			char ch = ' ';
			if (idx<key.length()) {
				ch = key.charAt(idx);
				if ((ch=='<') || (ch=='>')) {
					format = formatBrakes;
				}
			}
			
			context.writeChar(ch, format);
		}
	}



	public static class ButtonModel {
		private final int limit;
		private boolean isDirty = true;
		private List<KeyAction> actions = new ArrayList<>();
		private List<ButtonColumn> columns = new ArrayList<>();
		private int visibileActions;
		
		public ButtonModel(int limit) {
			this.limit = limit;
		}
		
		
		public void validate() {
			if (!isDirty) {
				return;
			}
			
			columns.clear();
			isDirty = false;
			
			int listIndex = 0;
			int keySize = 0;
			int labelSize = 0;

			boolean hasShift = false;
			for(int index=0; index<actions.size(); index++) {
				KeyAction action = actions.get(index);
				if (!action.shown) {
					continue;
				}
				if (listIndex>0 && (listIndex%limit) == 0) {
					columns.add(new ButtonColumn(keySize, labelSize, hasShift));
					keySize = 0;
					labelSize = 0;
				}
				listIndex++;
				
				int keyLength = action.key.length();
				if (keySize<keyLength) {
					keySize = keyLength;
				}

				if (action.faceAction instanceof CheckBox) {
					if (!hasShift) {
						labelSize++;
						hasShift = true;
					}
				}

				
				int labelLength = action.faceAction.getPreferredWidth();
				if (labelSize<labelLength) {
					labelSize = labelLength;
				}
			}

			if (keySize>0 || labelSize>0) {
				columns.add(new ButtonColumn(keySize, labelSize, hasShift));
			}
			visibileActions = listIndex;
		}

	}

	
	public static class ButtonColumn {
		public final int keySize;
		public final int textSize;
		public final boolean hasShift;
		public ButtonColumn(int keySize, int textSize, boolean hasShift) {
			this.keySize = keySize;
			this.textSize = textSize;
			this.hasShift = hasShift;
		}
		
		public int buttonSize(int padding) {
			if (keySize==0 || textSize==0) {
				return keySize + textSize;
			}
			return keySize + textSize + padding;
		}
	}
	
}
