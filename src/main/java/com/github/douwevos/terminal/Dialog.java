package com.github.douwevos.terminal;

import java.util.List;
import java.util.Objects;

import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.FaceContainer;
import com.github.douwevos.terminal.component.KeyAction;

public class Dialog extends FaceContainer implements KeyActionListObserver {

	private List<KeyAction> actions;
	
	public Dialog(Face child) {
		addChild(child);
	}
	
	
	@Override
	public void updateLayout(int x, int y, int width, int height) {
		super.updateLayout(x, y, width, height);
		streamChildren().forEach(s -> s.updateLayout(x, y, width, height));
	}
	
//	@Override
//	public void update(UpdateContext context) {
//		boolean parentCleared = (getUpdateFlags() & (UF_CLEAR|UF_PARENT_CLEARED)) != 0;
//		super.update(context);
//		if (parentCleared) {
//			child.markUpdateFlag(UF_PARENT_CLEARED);
//		}
//		int childUpdateFlags = child.getRecursiveUpdateFlags();
//		if ((childUpdateFlags & (UF_CLEAR|UF_PAINT|UF_PARENT_CLEARED)) != 0) {
//			UpdateContext contextSub = context.withFace(child);
//			child.update(contextSub);
//		}
//	}
//	
//	
//	@Override
//	public void enlistFocusElements(List<Face> focusElements) {
//		child.enlistFocusElements(focusElements);
//	}
//	
	
	@Override
	public void enlistKeyActions(KeyActionCollector keyActionCollector) {
		super.enlistKeyActions(keyActionCollector);
		keyActionCollector.add(this);
	}
	
	
	@Override
	public void onNewKeyActionList(KeyActionCollector keyActionCollector) {
		actions = keyActionCollector.keyActions;
	}
	
	@Override
	public int getPreferredHeight() {
		return streamChildren().mapToInt(s -> s.getPreferredHeight()).max().orElse(0);
	}
	
	
	@Override
	public int getPreferredWidth() {
		return streamChildren().mapToInt(s -> s.getPreferredWidth()).max().orElse(0);
	}
	
	
	@Override
	public boolean onKeyEvent(KeyEvent keyEvent) {
		if (super.onKeyEvent(keyEvent)) {
			return true;
		}
		if (actions!=null) {
			String keyText = keyEvent.toKeyText();
			for(KeyAction keyAction : actions) {
				if (Objects.equals(keyAction.key, keyText) && keyAction.runnable!=null) {
					keyAction.fire();
					return true;
				}
			}
		}
		return true;
	}
	
	
	@Override
	public void paint(UpdateContext context) {
	}
}
