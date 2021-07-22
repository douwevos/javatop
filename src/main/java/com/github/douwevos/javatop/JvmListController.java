package com.github.douwevos.javatop;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.javatop.JvmService.JvmDescription;
import com.github.douwevos.javatop.ProcessSampler.PsInfo;
import com.github.douwevos.javatop.ProcessSampler.Stat;
import com.github.douwevos.javatop.filter.FilterConfigController;
import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.KeyEvent;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.component.ButtonBar;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.Menu;
import com.github.douwevos.terminal.component.PropertyList;
import com.github.douwevos.terminal.component.PropertyList.Property;
import com.github.douwevos.terminal.component.Splitter;
import com.github.douwevos.terminal.component.Text;

public class JvmListController implements Controller, ProcessSampler.Observer {

	private final TerminalWindow terminalWindow;

	private final KeyAction actionRefresh = new KeyAction("<F5>", new Label("refresh VMs"), this::updateJvmList);
	private final KeyAction actionFilterConfig = new KeyAction("<c>", new Label("filter config"), this::showFilterConfig);
	
	public void doToggle() {
	}
	
	private List<JvmDescription> vms = new ArrayList<>();
	
	private Menu menuVmList;
	private Face mainRoot;
	Splitter splitterProcessInfo;
	
	private ProcessSampler processSampler;

	PropertyList propertyListStat = new PropertyList();
	PropertyList propertyListPs = new PropertyList();

	public JvmListController(TerminalWindow terminalWindow) {
		this.terminalWindow = terminalWindow;
	}
	
	@Override
	public TerminalWindow getTerminalWindow() {
		return terminalWindow;
	}

	private Face createEventKeyLog() {
		Text text = new Text() {
			List<KeyEvent> events = new ArrayList<>();
			
			@Override
			public boolean onKeyEvent(KeyEvent keyEvent) {
				events.add(keyEvent);
				if (events.size()>10) {
					events.remove(0);
				}
				StringBuilder buf = new StringBuilder();
				for(KeyEvent e : events) {
					buf.append(e.toString()).append("\n");
				}
				setText(buf.toString());
				return true;
			}
		};
		return text;
	}

	private Face getOrCreateMainRoot() {
		if (mainRoot == null) {
			
			propertyListPs.setKeyFormat(new AnsiFormat(-1, AnsiFormat.ANSI_CYAN, false));
			propertyListStat.setKeyFormat(new AnsiFormat(-1, AnsiFormat.ANSI_GREEN, false));

			
			PropertyList properties = new PropertyList();
			properties.setKeyFormat(new AnsiFormat(-1, 6, false));
			properties.setCanFocus(false);
			properties.add(new Property("JavaTop", "Version 1"));
//			properties.add(new Property("JavaTop", "Version 1"));

			addSystemProperties(properties);
			
			Face frameButtons = createButtonBarFrame();
			
			menuVmList = new Menu() {
				@Override
				public void onItemSelected(int index, MenuEntry entry) {
					startJvmProfiler(index);
				}
				
				@Override
				public void setActivatedIndex(int newValue) {
					super.setActivatedIndex(newValue);
					MenuEntry activatedItem = getActivatedItem();
					Long processId = null;
					if (activatedItem!=null) {
						JvmDescription jvmDescription = vms.get(getActivatedIndex());
						try {
							processId = Long.parseLong(jvmDescription.vmId);
						} catch(NumberFormatException e) {
							// ignore
						}
					}
					processSampler.setProcessId(processId);
				}
				
			};

			propertyListStat.setCanFocus(false);
			propertyListPs.setCanFocus(false);
			splitterProcessInfo = new Splitter(propertyListStat, propertyListPs, Direction.HORIZONTAL, -1, true);
			

			Splitter splitterTop = new Splitter(frameButtons, properties, Direction.HORIZONTAL, -1, true);
			
//			Splitter splitterBotom = new Splitter(menuVmList, splitterProcessInfo, Direction.VERTICAL, 500, true);
			Splitter splitterBotom = new Splitter(menuVmList, createEventKeyLog(), Direction.VERTICAL, 500, true);
			
			
			Splitter splitterMain = new Splitter(splitterTop, splitterBotom, Direction.VERTICAL, -1, true) {
				
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionRefresh);
					keyActionCollector.add(actionFilterConfig);
					super.enlistKeyActions(keyActionCollector);
				}
			};
			
			mainRoot = splitterMain;
		}
		else {
			mainRoot.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		return mainRoot;
	}
	
	private void addSystemProperties(PropertyList properties) {
		try {

			String hostname = System.getenv("HOSTNAME");
			if (hostname==null) {
				hostname = readProcLine("/etc/hostname");
			}
			if (hostname != null) {
				properties.add(new Property("hostname", hostname));
			}
			
			String kernelversion = readProcLine("/proc/sys/kernel/version");
			if (kernelversion != null) {
				properties.add(new Property("kernel", kernelversion));
			}
			
		} catch(Throwable t) {
			// displaying properties are not that important :)
		}
	}
	
	private String readProcLine(String filename) {
		File kernelVersionFile = new File(filename);
		if (kernelVersionFile.exists() && kernelVersionFile.canRead()) {
			List<String> readAllLines;
			try {
				readAllLines = Files.readAllLines(kernelVersionFile.toPath());
				if (readAllLines!=null && !readAllLines.isEmpty()) {
					return readAllLines.get(0);
				}
			} catch (IOException e) {
			}
		}
		return null;
	}

	@Override
	public void show() {
		Face mainRoot = getOrCreateMainRoot();
		stopProcessSampler();
		processSampler = new ProcessSampler(this);
		updateJvmList();
		terminalWindow.setFace(mainRoot);
	}
	

	private void stopProcessSampler() {
		if (processSampler==null) {
			return;
		}
		processSampler.stop();
		processSampler = null;
	}

	private Face createButtonBarFrame() {
		ButtonBar buttonBar = new ButtonBar(5, 1, 2);
		buttonBar.setCanFocus(false);
		return buttonBar;
	}

	private void startJvmProfiler(int index) {
		stopProcessSampler();
		JvmMainMenuController jvmMainMenuController = new JvmMainMenuController(terminalWindow, vms.get(index)); 
		jvmMainMenuController.show();
	}
	
	
	@Override
	public void onNewStat(Stat stat, PsInfo psInfo) {
		propertyListStat.removeAll();
		if (stat!=null) {
			propertyListStat.add(new Property("threads", stat.numberOfThreads));
			propertyListStat.add(new Property("started at", stat.startTime));
			propertyListStat.add(new Property("state", stat.state));
			propertyListStat.add(new Property("user mode time", stat.userModeTime));
			propertyListStat.add(new Property("scheduled time", stat.scheduledTime));
			propertyListStat.add(new Property("priority", stat.priority));
			propertyListStat.add(new Property("nice", stat.nice));
		}
		
		propertyListPs.removeAll();
		if (psInfo != null) {
			propertyListPs.add(new Property("user", psInfo.user+"("+psInfo.uid+")"));
			propertyListPs.add(new Property("start", psInfo.startTime));
			propertyListPs.add(new Property("up", psInfo.upTime));
			propertyListPs.add(new Property("cpu", psInfo.percentageCpu));
			propertyListPs.add(new Property("mem", psInfo.percentageMem));

		}
		propertyListPs.markUpdateFlag(Face.UF_LAYOUT);
		propertyListStat.markUpdateFlag(Face.UF_LAYOUT);
		splitterProcessInfo.markUpdateFlag(Face.UF_PAINT);
	}
	
	private void updateJvmList() {
		vms = JvmService.enlistVirtualMachines();
		menuVmList.removeAll();
		
		for(JvmDescription desc : vms) {
			String text = String.format("%9s  %s", desc.vmId, desc.displayName);
			menuVmList.add(new Menu.MenuEntry(text, desc.canAttach));
		}
		menuVmList.setActivatedIndex(menuVmList.getActivatedIndex());
	}

	private void showFilterConfig() {
		FilterConfigController filterConfigController = new FilterConfigController(terminalWindow);
		filterConfigController.show();
	}
	
}
