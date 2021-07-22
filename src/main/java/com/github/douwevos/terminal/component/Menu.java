package com.github.douwevos.terminal.component;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.UpdateContext;
import com.github.douwevos.terminal.Rectangle;

public class Menu extends FaceComponentScrollable {

	private final MenuModel menuModel = new MenuModel();
	private final KeyAction actionCursorUp = new KeyAction("<up>", new Label("up"), this::cursorUp);
	private final KeyAction actionCursorDown = new KeyAction("<down>", new Label("down"), this::cursorDown);
	
	private int activatedItem = 0;


	
	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		if (hasFocus()) {
			keyActionCollector.add(actionCursorDown);
			keyActionCollector.add(actionCursorUp);
		}
	}
	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		
		switch(keyEvent.getKey()) {
			case 13 : onItemSelected(activatedItem, menuModel.get(activatedItem)); return true; 
		}
		return false;
	}

	public void cursorUp() {
		setActivatedIndex(activatedItem-1);
	}

	
	public void cursorDown() {
		setActivatedIndex(activatedItem+1);
	}
	
	public void setActivatedIndex(int newValue) {
		if (newValue>=menuModel.count()) {
			newValue = menuModel.count()-1;
		}
		if (newValue<0) {
			newValue = 0;
		}
		if (activatedItem == newValue) {
			return;
		}
		activatedItem = newValue;
		markUpdateFlag(UF_PAINT);
		ensureItemInView();
	}
	
	public int getActivatedIndex() {
		return activatedItem;
	}
	
	public MenuEntry getActivatedItem() {
		if (activatedItem<0 || activatedItem>=menuModel.count()) {
			return null;
		}
		return menuModel.items.get(activatedItem);
	}
	
	private void ensureItemInView() {
		ensureRectangleInView(new Rectangle(0,activatedItem,1,1));
	}

	public void onItemSelected(int index, MenuEntry entry) {
	}
	
	@Override
	public int getViewHeight() {
		return menuModel.items.size();
	}
	
	@Override
	public int getViewWidth() {
		return menuModel.items.stream().mapToInt(s -> s.text.length()).max().orElse(0);
	}

	@Override
	public void paint(UpdateContext context) {
		AnsiFormat formatNotSelectable = new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_RED, false);
		AnsiFormat format;

		
		int viewY = getViewY();
		int height = getHeight();
		int width = getWidth();
		for(int idx=0; idx<height; idx++) {
			int row = viewY+idx;
			if (row<menuModel.items.size()) {
				format = context.pickSelectFormat(hasFocus(), idx == activatedItem);
				
				MenuEntry menuEntry = menuModel.items.get(idx);
				String line = menuEntry.text;
				if (!menuEntry.selectable) {
					format = formatNotSelectable;
				}
				context.drawString(0, width, row, line, format, true);
			}
			else {
				context.clearLine(row, 0, width, context.getFormatDefault());
			}
		}
	}
	
	
	public void add(MenuEntry entry) {
		menuModel.add(entry);
		markUpdateFlag(UF_PAINT);
	}

	public void removeAll() {
		menuModel.removeAll();
		markUpdateFlag(UF_PAINT);
	}
	
	public static class MenuModel {
		private List<MenuEntry> items = new ArrayList<>();
		
		public void removeAll() {
			items.clear();
		}
		
		public MenuEntry get(int index) {
			return items.get(index);
		}

		public int count() {
			return items.size();
		}

		public void add(MenuEntry entry) {
			items.add(entry);
		}
	}
	
	public static class MenuEntry {
		
		private final String text;
		private final boolean selectable;
		
		public MenuEntry(String text) {
			this.text = text;
			this.selectable = true;
		}
		
		public MenuEntry(String text, boolean selectable) {
			this.text = text;
			this.selectable = selectable;
		}
		
		public String getText() {
			return text;
		}
		
		public boolean isSelectable() {
			return selectable;
		}
	}
	
}
