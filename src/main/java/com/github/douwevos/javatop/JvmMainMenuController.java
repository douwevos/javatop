package com.github.douwevos.javatop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.douwevos.javatop.JvmService.JvmDescription;
import com.github.douwevos.javatop.JvmService.RuntimeInfo;
import com.github.douwevos.javatop.block.JvmBlockController;
import com.github.douwevos.javatop.profiler.JvmProfileController;
import com.github.douwevos.javatop.thread.JvmThreadMonitorController;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.component.ButtonBar;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.PropertyList;
import com.github.douwevos.terminal.component.PropertyList.Property;
import com.github.douwevos.terminal.component.Splitter;

public class JvmMainMenuController implements Controller {

	private final TerminalWindow terminalWindow;
	private final JvmDescription jvmDescription;
	private final JvmService jvmService; 

	private final AnsiFormat brightWhite =  new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_WHITE, false);
	
	private final KeyAction actionBack = new KeyAction("<esc>", new Label("jvm list", new AnsiFormat(-1, AnsiFormat.ANSI_MAGENTA, false)), this::returnToJvmList);
	private final KeyAction actionStartProfiler = new KeyAction("<p>", new Label("profile", brightWhite), this::startProfiler);
	private final KeyAction actionStartThreadMonitor = new KeyAction("<t>", new Label("threads", brightWhite), this::startThreadMonitor);
	private final KeyAction actionStartBlockMonitor = new KeyAction("<b>", new Label("blocker", brightWhite), this::startBlockMonitor);
	
	private PropertyList properties;
	
	private PropertyList jvmSystemPropertiesList;
	
	private Face mainRoot;
	
	public JvmMainMenuController(TerminalWindow terminalWindow, JvmDescription jvmDescription) {
		this.terminalWindow = terminalWindow;
		this.jvmDescription = jvmDescription;
		jvmService = new JvmService(jvmDescription);
		jvmService.connect();
	}
	
	
	private Face getOrCreateMainRoot() {
		if (mainRoot == null) {
			properties = new PropertyList();
			properties.setKeyFormat(new AnsiFormat(-1, 6, false));
			properties.setCanFocus(false);
			
			Face frameButtons = createButtonBarFrame(); 
			jvmSystemPropertiesList = new PropertyList();
			jvmSystemPropertiesList.setKeyFormat(new AnsiFormat(-1, AnsiFormat.ANSI_GREEN, false));

			Splitter splitterTop = new Splitter(frameButtons, properties, Direction.HORIZONTAL, -1, true);
			Splitter splitterMain = new Splitter(splitterTop, jvmSystemPropertiesList, Direction.VERTICAL, -1, true) {
				
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionBack);
					keyActionCollector.add(actionStartProfiler);
					keyActionCollector.add(actionStartThreadMonitor);
					keyActionCollector.add(actionStartBlockMonitor);
					super.enlistKeyActions(keyActionCollector);
				}
				
			};
			
//			Splitter splitterRight = new Splitter(properties, text, Direction.VERTICAL, -1);
//			Splitter splitter = new Splitter(frameButtons, splitterRight, Direction.HORIZONTAL, -1);
			mainRoot = splitterMain;
		}
		else {
			properties.removeAll();
			mainRoot.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		fillRuntimeProperties(properties);
		return mainRoot;
	}
	

	@Override
	public void show() {
		terminalWindow.setFace(getOrCreateMainRoot());
	}
	
	@Override
	public TerminalWindow getTerminalWindow() {
		return terminalWindow;
	}

	private Face createButtonBarFrame() {
		ButtonBar buttonBar = new ButtonBar(7, 1, 2);
		
		return buttonBar;
//		Frame frameButtons = new Frame(buttonBar, "Keys");
//		return frameButtons;
	}

	private void fillRuntimeProperties(PropertyList properties) {
		properties.removeAll();
		jvmSystemPropertiesList.removeAll();
		properties.add(new Property("vm-name", jvmDescription.displayName));
		RuntimeInfo runtimeInfo = jvmService.getRuntimeInfo();
		if (runtimeInfo == null) {
			return;
		}
		properties.add(new Property("name", runtimeInfo.name));
		properties.add(new Property("PID", ""+runtimeInfo.pid));
		properties.add(new Property("uptime", runtimeInfo.uptime.toString()));
		properties.add(new Property("VM-name", runtimeInfo.vmName));
		properties.add(new Property("VM-vendor", runtimeInfo.vmVendor));
		properties.add(new Property("VM-version", runtimeInfo.vmVersion));
		
		
		Map<String, String> systemProperties = runtimeInfo.systemProperties;
		if (systemProperties == null) {
			return;
		}
		List<String> keys = new ArrayList<String>(systemProperties.keySet());
		Collections.sort(keys);
		for(String key : keys) {
			String value = systemProperties.get(key);
			jvmSystemPropertiesList.add(new Property(key, value));
		}
	}


	private void returnToJvmList() {
		JvmListController listController = new JvmListController(terminalWindow);
		listController.show();
	}

	private void startProfiler() {
		JvmProfileController jvmProfiler = new JvmProfileController(terminalWindow, this, jvmService, properties);
		jvmProfiler.show();
	}

	private void startThreadMonitor() {
		JvmThreadMonitorController controller = new JvmThreadMonitorController(this, jvmService, properties);
		controller.show();
	}

	private void startBlockMonitor() {
		JvmBlockController controller = new JvmBlockController(this, jvmService, properties);
		controller.show();
	}

}
