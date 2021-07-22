package com.github.douwevos.terminal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.UpdateContext;

public abstract class FaceContainer extends Face {

	protected final List<Face> children = new ArrayList<>();

	public Stream<Face> streamChildren() {
		return children.stream();
	}
	
	public int count() {
		return children.size();
	}
	
	public Face childAt(int index) {
		return children.get(index);
	}

	public void addChild(Face child) {
		children.add(child);
		child.markUpdateFlag(UF_LAYOUT | UF_CLEAR);
	}

	@Override
	public void clear(UpdateContext context) {
		super.clear(context);
		children.stream().forEach(s -> s.markUpdateFlag(UF_PAINT));
	}
	
	@Override
	public void enlistFocusElements(List<Face> focusElements) {
		streamChildren().forEach(f -> f.enlistFocusElements(focusElements));
	}
	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		return streamChildren().anyMatch(s -> s.onKeyEvent(keyEvent));
	}

	@Override
	public int getRecursiveUpdateFlags() {
		int childFlags = streamChildren().mapToInt(s -> s.getRecursiveUpdateFlags()).reduce((a,b) -> a|b).orElse(0);
		return childFlags | getUpdateFlags();
	}
	
	@Override
	public void update(UpdateContext context) {
		boolean parentCleared = (getUpdateFlags() & (UF_CLEAR|UF_PARENT_CLEARED)) != 0;
		super.update(context);
		for(Face child : children) {
			if (parentCleared) {
				child.markUpdateFlag(UF_PARENT_CLEARED);
			}
			int childUpdateFlags = child.getRecursiveUpdateFlags();
			if ((childUpdateFlags & (UF_CLEAR|UF_PAINT|UF_PARENT_CLEARED)) != 0) {
				UpdateContext contextSub = context.withFace(child);
				child.update(contextSub);
			}
		}
		
	}

	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		streamChildren().forEach(c -> c.enlistKeyActions(keyActionCollector));
	}
	
}
