package com.github.douwevos.javatop.profiler;

import java.util.List;

import com.github.douwevos.javatop.Controller;
import com.github.douwevos.javatop.JvmService;
import com.github.douwevos.javatop.profiler.JvmProfilerSampler.MethodCallInfo;
import com.github.douwevos.javatop.profiler.JvmProfilerSampler.MethodCallInfoWithLines;
import com.github.douwevos.javatop.profiler.JvmProfilerSampler.ProfileSnapshot;
import com.github.douwevos.terminal.Direction;
import com.github.douwevos.terminal.KeyActionCollector;
import com.github.douwevos.terminal.TerminalWindow;
import com.github.douwevos.terminal.component.ButtonBar;
import com.github.douwevos.terminal.component.CheckBox;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.Frame;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.Menu;
import com.github.douwevos.terminal.component.Menu.MenuEntry;
import com.github.douwevos.terminal.component.PropertyList;
import com.github.douwevos.terminal.component.Splitter;
import com.github.douwevos.terminal.component.Text;

public class JvmProfileController implements JvmProfilerSampler.ProfileSnapshotObserver, Controller {

	private final Controller parentController;
	private final TerminalWindow terminalWindow;
	private final JvmService jvmService;
	private final PropertyList jvmPropertyList;
	
	private final KeyAction actionBack = new KeyAction("<esc>", new Label("jvm menu"), this::returnToJvmMenu);
	private final KeyAction actionPause = new KeyAction("<space>", new CheckBox("pause"), this::togglePause);
	private final KeyAction actionInvokers = new KeyAction("<i>", new CheckBox("invokers"), this::toggleInvokers);

	private JvmProfilerSampler jvmProfilerSampler;

	private Face mainFace;

	private Menu menuResults;
	private Text textMethodDetails;
	private Frame focusFrame;
	
	public JvmProfileController(TerminalWindow terminalWindow, Controller parentController, JvmService jvmService, PropertyList jvmPropertyList) {
		this.parentController = parentController;
		this.terminalWindow = terminalWindow;
		this.jvmService = jvmService;
		this.jvmPropertyList = jvmPropertyList;
	}

	private void returnToJvmMenu() {
		jvmProfilerSampler.stop();
		parentController.show();
	}

	private void togglePause() {
		boolean paused = jvmProfilerSampler.togglePause();
		CheckBox c = ((CheckBox) actionPause.faceAction);
		c.setChecked(paused);
	}

	private void toggleInvokers() {
		CheckBox c = ((CheckBox) actionInvokers.faceAction);
		if (c.isChecked()) {
			MenuEntry activatedItem = menuResults.getActivatedItem();
			if (activatedItem == null) {
				c.toggle();
				return;
			}
			String fqClassAndMethodName = getFqClassAndMethodName(activatedItem.getText());
			jvmProfilerSampler.setInvokersFqname(fqClassAndMethodName);
		} else {
			jvmProfilerSampler.setInvokersFqname(null);
		}
	}
	
	private void selectProfileLine(int index, MenuEntry item) {
		String fqname = getFqClassAndMethodName(item.getText());
		jvmProfilerSampler.setFocusFqname(fqname);
		
		focusFrame.addChild(textMethodDetails);
		focusFrame.markUpdateFlag(Face.UF_PAINT);
	}
	
	public String getFqClassAndMethodName(String menuText) {
		String[] split = menuText.split(" ");
		return split[split.length-1];
		
	}

	
	private void showNewSnapshot() {
		ProfileSnapshot profileSnapshot = jvmProfilerSampler==null ? null : jvmProfilerSampler.getProfileSnapshot();
		menuResults.removeAll();
		if (profileSnapshot != null) {
//			menuResults.add(new MenuEntry(jvmDescription.vmId+"   "+jvmDescription.displayName, false));
			List<MethodCallInfo> methodCallInfos = profileSnapshot.getMethodCallInfos();
			long lcTotalMethodCpuTime = profileSnapshot.getTotalMethodCpuTime();

			for(MethodCallInfo mci : methodCallInfos) {
				double ratio = (double) mci.cpuTime / lcTotalMethodCpuTime * 100;
				String t = String.format("%6.2f%% %s.%s", ratio, mci.className,mci.methodName);
				menuResults.add(new MenuEntry(t, true));
			}
			String focusFqname = jvmProfilerSampler.getFocusFqname();
			
			MethodCallInfoWithLines focusMethod = profileSnapshot.getFocusMethod();
			if (focusMethod!=null && focusMethod.getLinesFreq()!=null) {
				int[] linesFreq = focusMethod.getLinesFreq();
				int max = 0;
				for(int idx=0; idx<linesFreq.length; idx++) {
					if (max<linesFreq[idx]) {
						max = linesFreq[idx];
					}
				}

				int barWidth = textMethodDetails.getWidth()-7;
				if (barWidth<3) {
					barWidth = 3;
				}
				
				StringBuilder buf = new StringBuilder();
				buf.append("Method details ").append(focusFqname).append("\n");
				for(int idx=0; idx<linesFreq.length; idx++) {
					String num = "       "+(idx + focusMethod.getSmallestLineNumber());
					buf.append(num.substring(num.length()-6));
					buf.append(' ');
					
					char blocks[] = new char[] {
							'▏', '▎', '▍', '▌', '▋', '▊', '▉'
					};
					
					int sp = (8 * barWidth*linesFreq[idx])/max;
					int fullParts = sp/8;
					for(int s=0; s<fullParts; s++) {
						buf.append('█');
					}
					int rest = sp - fullParts*8;
					rest--;
					if (rest>=0 && rest<blocks.length) {
						buf.append(blocks[rest]);
					}
					
					buf.append('\n');
				}
				textMethodDetails.setText(buf.toString());
				textMethodDetails.markUpdateFlag(Face.UF_PAINT);
			} else {
				textMethodDetails.setText(focusFqname);
				textMethodDetails.markUpdateFlag(Face.UF_PAINT);
				
			}
			
		}
		menuResults.markUpdateFlag(Face.UF_PAINT);
	}
	
	@Override
	public void onNewSnapshot(ProfileSnapshot snapshot) {
		terminalWindow.invokeLater(this::showNewSnapshot);
	}
	
	private Face getOrCreateMainRoot() {
		if (mainFace == null) {

			menuResults = new Menu() {
				
				@Override
				public void onItemSelected(int index, MenuEntry entry) {
					selectProfileLine(index, entry);
				}
			};
			
			textMethodDetails = new Text();
			textMethodDetails.setText("here a little text");
			
			focusFrame = new Frame(textMethodDetails, "focus");
			Face frameButtons = new ButtonBar(7,1,1); 

			Splitter splitterTop = new Splitter(frameButtons, jvmPropertyList, Direction.HORIZONTAL, -1, true);
			Splitter splitterBottom = new Splitter(menuResults, focusFrame, Direction.HORIZONTAL, 700, false);
			
			Splitter mainSplitter = new Splitter(splitterTop, splitterBottom, Direction.VERTICAL, -1, true) {
				
				
				@Override
				public void enlistKeyActions(KeyActionCollector keyActionCollector) {
					keyActionCollector.add(actionBack);
					keyActionCollector.add(actionPause);
					keyActionCollector.add(actionInvokers);
					super.enlistKeyActions(keyActionCollector);
				}
				
			};
			
			jvmPropertyList.markUpdateFlag(Face.UF_PAINT| Face.UF_LAYOUT);
			
			mainFace = mainSplitter;
		}
		else {
			mainFace.markUpdateFlag(Face.UF_LAYOUT|Face.UF_CLEAR);
		}
		return mainFace;
	}

	public void show() {
		terminalWindow.setFace(getOrCreateMainRoot());
		jvmProfilerSampler = new JvmProfilerSampler(jvmService, this);
	}
	
	@Override
	public TerminalWindow getTerminalWindow() {
		return parentController.getTerminalWindow();
	}
}
