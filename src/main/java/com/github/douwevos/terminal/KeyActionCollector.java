package com.github.douwevos.terminal;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.component.KeyAction;

public class KeyActionCollector {

	public List<KeyAction> keyActions = new ArrayList<>();
	
	public List<KeyActionListObserver> observers = new ArrayList<>();
	
	
	public void add(KeyAction keyAction) {
		keyActions.add(keyAction);
	}
	
	public void add(KeyActionListObserver observer) {
		observers.add(observer);
	}
	
	public void notifyObservers() {
		observers.stream().forEach(o -> o.onNewKeyActionList(this));
	}
	
}
