package com.github.douwevos.javatop.block;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.javatop.Controller;
import com.github.douwevos.javatop.JvmService;
import com.github.douwevos.javatop.block.FaceBlockList.ModelItem;
import com.github.douwevos.javatop.block.JvmBlockSampler.BlockClassDetails;
import com.github.douwevos.javatop.block.JvmBlockSampler.BlockInfoMap;
import com.github.douwevos.javatop.block.JvmBlockSampler.BlockListSnapshotObserver;
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
import com.github.douwevos.terminal.component.TextView;

public class JvmBlockController implements Controller, BlockListSnapshotObserver, FaceBlockList.Observer {

	private final Controller parentController;
	private final JvmService jvmService;
	private final PropertyList jvmPropertyList;

	private final KeyAction actionBack = new KeyAction("<esc>", new Label("jvm menu"), this::returnToJvmMenu);
	private final KeyAction actionPause = new KeyAction("<space>", new CheckBox("pause"), this::togglePause);
	private final KeyAction actionFilter = new KeyAction("<f>", new CheckBox("filter"), this::toggleFilter);

	private ModelItem selectedModelItem;
	
	private FaceBlockList faceBlockList;
	
	private TextView stacktraceList;
	
	private volatile JvmBlockSampler jvmBlockSampler;

	private Face mainFace;

	public JvmBlockController(Controller parentController, JvmService jvmService, PropertyList jvmPropertyList) {
		this.parentController = parentController;
		this.jvmService = jvmService;
		this.jvmPropertyList = jvmPropertyList;
		((CheckBox) actionFilter.faceAction).setChecked(true);
	}

	private void showNewSnapshot() {
		JvmBlockSampler jvmBlockSampler = this.jvmBlockSampler;
		if (jvmBlockSampler ==null || faceBlockList==null) {
			return;
		}
		List<BlockInfoMap> snapshot = jvmBlockSampler.getSnapshot();
		if (snapshot == null) {
			return;
		}
		faceBlockList.setSnapshot(snapshot);
	}


	private void returnToJvmMenu() {
		jvmBlockSampler.stop();
		parentController.show();
	}
	
	private void togglePause() {
		boolean paused = jvmBlockSampler.togglePause();
		CheckBox c = ((CheckBox) actionPause.faceAction);
		c.setChecked(paused);
	}

	private void toggleFilter() {
		updateStacktraceDump();
	}

	private Face getOrCreateMainRoot() {
		if (mainFace == null) {
			faceBlockList = new FaceBlockList(this);
			Face frameButtons = new ButtonBar(7,1,1); 

			stacktraceList = new TextView();
			Splitter splitterListAndDetails = new Splitter(faceBlockList, stacktraceList, Direction.HORIZONTAL, 500, true);
			
			Splitter topSplitter = new Splitter(frameButtons, jvmPropertyList, Direction.HORIZONTAL, -1, true);
			Splitter mainSplitter = new Splitter(topSplitter, splitterListAndDetails, Direction.VERTICAL, -1, true) {
				
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionBack);
					keyActionCollector.add(actionPause);
					keyActionCollector.add(actionFilter);
					super.enlistKeyActions(keyActionCollector);
				}
				
			};

			mainFace = mainSplitter;
		}
		else {
			mainFace.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		return mainFace;
	}

	
	@Override
	public void show() {
		jvmBlockSampler = new JvmBlockSampler(jvmService, this);
		getTerminalWindow().setFace(getOrCreateMainRoot());
	}

	@Override
	public TerminalWindow getTerminalWindow() {
		return parentController.getTerminalWindow();
	}

	
	@Override
	public void onNewSnapshot(List<BlockInfoMap> snapshot) {
		getTerminalWindow().invokeLater(this::showNewSnapshot);
	}

	@Override
	public void onNewSelection(ModelItem item) {
		selectedModelItem = item;
		updateStacktraceDump();
	}
		
	
	public void updateStacktraceDump() {
		ModelItem item = selectedModelItem;
		if (item == null) {
			return;
		}
		
		boolean doFilter = ((CheckBox) actionFilter.faceAction).isChecked();
		
		List<BlockClassDetails> blocked = item.blockInfo.getBlocked();
		List<String> lines = new ArrayList<>();
		for(BlockClassDetails bcd : blocked)  {
			lines.add("Thread(" + bcd.getThreadId() + ")");
			StackTraceElement[] stackTrace = bcd.getStackTrace();
			if (stackTrace!=null) {
				int idx = 1;
				for(StackTraceElement ste : stackTrace) {
					String t = String.format("  %3d %s.%s(%d)", idx, ste.getClassName(), ste.getMethodName(), ste.getLineNumber());
					lines.add(t);
					idx++;
					if (doFilter && idx>3) {
						break;
					}
				}
			}
			lines.add("");
		}
		stacktraceList.setText(lines);
		
	}
	
}
