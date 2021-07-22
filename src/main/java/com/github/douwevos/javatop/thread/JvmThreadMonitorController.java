package com.github.douwevos.javatop.thread;

import com.github.douwevos.javatop.Controller;
import com.github.douwevos.javatop.JvmService;
import com.github.douwevos.javatop.thread.JvmThreadSampler.SampleMode;
import com.github.douwevos.javatop.thread.JvmThreadSampler.ThreadListSnapshot;
import com.github.douwevos.javatop.thread.JvmThreadSampler.ThreadListSnapshotObserver;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.component.ButtonBar;
import com.github.douwevos.terminal.component.CheckBox;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.PropertyList;
import com.github.douwevos.terminal.component.Splitter;

public class JvmThreadMonitorController implements Controller, ThreadListSnapshotObserver {

	private final Controller parentController;
	private final JvmService jvmService;
	private final PropertyList jvmPropertyList;
	
	private final KeyAction actionBack = new KeyAction("<esc>", new Label("jvm list", new AnsiFormat(-1, AnsiFormat.ANSI_MAGENTA, false)), this::returnToJvmMenu);
	private final KeyAction actionPause = new KeyAction("<space>", new CheckBox("pause"), this::togglePause);
	private final KeyAction actionModeCpuTime = new KeyAction("<c>", new CheckBox("cpu time"), this::modeCpuTime);
	private final KeyAction actionModeThreadState = new KeyAction("<s>", new CheckBox("thread state"), this::modeThreadState);

	private volatile JvmThreadSampler jvmThreadSampler;
	
	private FaceThreadList faceThreadList;
	
	private Face mainFace;
	
	public JvmThreadMonitorController(Controller parentController, JvmService jvmService, PropertyList jvmPropertyList) {
		this.parentController = parentController;
		this.jvmService = jvmService;
		this.jvmPropertyList = jvmPropertyList;
	}

	private void showNewSnapshot() {
		JvmThreadSampler jvmThreadSampler = this.jvmThreadSampler;
		if (jvmThreadSampler ==null || faceThreadList==null) {
			return;
		}
		ThreadListSnapshot threadListSnapshot = jvmThreadSampler.getThreadListSnapshot();
		if (threadListSnapshot == null) {
			return;
		}
		faceThreadList.setSnapshot(threadListSnapshot);
	}


	
	@Override
	public TerminalWindow getTerminalWindow() {
		return parentController.getTerminalWindow();
	}

	private void returnToJvmMenu() {
		jvmThreadSampler.stop();
		parentController.show();
	}
	
	private void togglePause() {
		boolean paused = jvmThreadSampler.togglePause();
		CheckBox c = ((CheckBox) actionPause.faceAction);
		c.setChecked(paused);
	}
	
	private void modeCpuTime() {
		jvmThreadSampler.setSampleMode(SampleMode.CPU_TIME);
		faceThreadList.setSampleMode(SampleMode.CPU_TIME);
		setToggle(actionModeCpuTime, true);
		setToggle(actionModeThreadState, false);
	}

	private void modeThreadState() {
		jvmThreadSampler.setSampleMode(SampleMode.THREAD_STATES);
		faceThreadList.setSampleMode(SampleMode.THREAD_STATES);
		setToggle(actionModeCpuTime, false);
		setToggle(actionModeThreadState, true);
	}

	private void setToggle(KeyAction action, boolean value) {
		((CheckBox) action.faceAction).setChecked(value);
	}
	
	private Face getOrCreateMainRoot() {
		if (mainFace == null) {
			faceThreadList = new FaceThreadList();
			Face frameButtons = new ButtonBar(5,1,2); 

			
			Splitter topSplitter = new Splitter(frameButtons, jvmPropertyList, Direction.HORIZONTAL, -1, true);
			Splitter mainSplitter = new Splitter(topSplitter, faceThreadList, Direction.VERTICAL, -1, true) {
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionBack);
					keyActionCollector.add(actionPause);
					keyActionCollector.add(actionModeCpuTime);
					keyActionCollector.add(actionModeThreadState);
					super.enlistKeyActions(keyActionCollector);
				}
			};
			
//			Splitter splitterProperties = new Splitter(jvmPropertyList, faceThreadList, Direction.VERTICAL, -1);
//			Splitter mainSplitter = new Splitter(frameButtons, splitterProperties, Direction.HORIZONTAL, -1);

			mainFace = mainSplitter;
		}
		else {
			mainFace.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		return mainFace;
	}

	
	@Override
	public void show() {
		jvmThreadSampler = new JvmThreadSampler(jvmService, this);
		Face mainRoot = getOrCreateMainRoot();
		modeCpuTime();
		getTerminalWindow().setFace(mainRoot);

	}
	
	@Override
	public void onNewSnapshot(ThreadListSnapshot snapshot) {
		getTerminalWindow().invokeLater(this::showNewSnapshot);
	}
	
}
