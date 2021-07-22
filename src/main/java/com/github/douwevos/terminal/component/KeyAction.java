package com.github.douwevos.terminal.component;

public class KeyAction {

	public final String key;
	public final Face faceAction;
	public final Runnable runnable;
	public final boolean shown;
	
	public KeyAction(String key, Face faceAction, Runnable runnable) {
		this.key = key;
		this.faceAction = faceAction;
		this.runnable = runnable;
		this.shown = true;
	}

	public KeyAction(String key, Face faceAction, Runnable runnable, boolean shown) {
		this.key = key;
		this.faceAction = faceAction;
		this.runnable = runnable;
		this.shown = shown;
	}
	
	public void fire() {
		if (faceAction instanceof CheckBox) {
			CheckBox checkBox = (CheckBox) faceAction;
			checkBox.toggle();
		}
		if (runnable!=null) {
			runnable.run();
		}
	}

}
