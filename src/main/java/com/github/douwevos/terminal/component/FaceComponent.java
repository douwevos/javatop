package com.github.douwevos.terminal.component;

import java.util.List;

import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;

public abstract class FaceComponent extends Face {

	protected boolean canFocus = true;
	
	@Override
	public void enlistFocusElements(List<Face> focusElements) {
		if (canFocus) {
			focusElements.add(this);
		}
	}
	
	public void setCanFocus(boolean canFocus) {
		if (this.canFocus == canFocus) {
			return;
		}
		this.canFocus = canFocus;
	}

	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		return false;
	}
	
	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
	}
}
