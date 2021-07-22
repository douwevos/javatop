package com.github.douwevos.javatop.filter;

import com.github.douwevos.javatop.Controller;
import com.github.douwevos.javatop.JvmListController;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.component.ButtonBar;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.Frame;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.Splitter;

public class FilterConfigController implements Controller {

	private final KeyAction actionBack = new KeyAction("<esc>", new Label("jvm list", new AnsiFormat(-1, AnsiFormat.ANSI_MAGENTA, false)), this::returnToJvmList);

	private final TerminalWindow terminalWindow;
	
	private Face mainFace;
	
	public FilterConfigController(TerminalWindow terminalWindow) {
		this.terminalWindow = terminalWindow;
	}
	
	private Face getOrCreateMainRoot() {
		if (mainFace == null) {
//			faceBlockList = new FaceBlockList(this);
			Face frameButtons = new ButtonBar(3,1,1); 
//
//			stacktraceList = new TextView();
			FilterList textView = new FilterList(getTerminalWindow());
//			Splitter splitterListAndDetails = new Splitter(faceBlockList, stacktraceList, Direction.HORIZONTAL, 500, true);
//			
//			Splitter topSplitter = new Splitter(frameButtons, jvmPropertyList, Direction.HORIZONTAL, -1, true);
			Frame frame = new Frame(textView, "Filter patterns");
			Splitter mainSplitter = new Splitter(frameButtons, frame, Direction.VERTICAL, -1, true) {
//				
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionBack);
//					keyActionCollector.add(actionPause);
//					keyActionCollector.add(actionFilter);
					super.enlistKeyActions(keyActionCollector);
				}
//				
			};
//
			mainFace = mainSplitter;
		}
		else {
			mainFace.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		return mainFace;
	}
	@Override
	public void show() {
		getTerminalWindow().setFace(getOrCreateMainRoot());
	}
	
	@Override
	public TerminalWindow getTerminalWindow() {
		return terminalWindow;
	}
	
	
	

	private void returnToJvmList() {
		JvmListController listController = new JvmListController(terminalWindow);
		listController.show();
	}

}
