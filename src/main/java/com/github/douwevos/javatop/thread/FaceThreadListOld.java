//package com.github.douwevos.javatop.thread;
//
//import java.util.List;
//
//import com.github.douwevos.javatop.thread.JvmThreadSampler.SampleMode;
//import com.github.douwevos.javatop.thread.JvmThreadSampler.ThreadListSnapshot;
//import com.github.douwevos.javatop.thread.JvmThreadSampler.ThreadMonitorInfo;
//import com.github.douwevos.javatop.thread.JvmThreadSampler.ThreadSample;
//import com.github.douwevos.terminal.AnsiFormat;
//import com.github.douwevos.terminal.KeyEvent;
//import com.github.douwevos.terminal.UpdateContext;
//import com.github.douwevos.terminal.KeyEvent.KeyCode;
//import com.github.douwevos.terminal.Rectangle;
//import com.github.douwevos.terminal.component.FaceComponent;
//import com.github.douwevos.terminal.component.Scrollable;
//
//public class FaceThreadListOld extends FaceComponent implements Scrollable {
//
//	private AnsiFormat formatStateNull = new AnsiFormat(-1, -1, false);
//	private AnsiFormat formatStateNew = new AnsiFormat(AnsiFormat.ANSI_WHITE, -1, false);
//	private AnsiFormat formatStateRunnable = new AnsiFormat(AnsiFormat.ANSI_GREEN, -1, false);
//	private AnsiFormat formatStateBlocked = new AnsiFormat(AnsiFormat.ANSI_RED, -1, false);
//	private AnsiFormat formatStateWaiting = new AnsiFormat(AnsiFormat.ANSI_BLUE, -1, false);
//	private AnsiFormat formatStateTimedWaiting = new AnsiFormat(AnsiFormat.ANSI_BRIGHT_BLUE, -1, false);
//	private AnsiFormat formatStateTerminated = new AnsiFormat(AnsiFormat.ANSI_GRAY, -1, false);
//	
//	private ThreadListSnapshot snapshot;
//	
//	private int highlightedItem = 0;
//	private int itemHeight = 3;
//	
//	private int viewX;
//	private int viewY;
//	private int viewHeight;
//	private int viewWidth;
//	private int nameColumnSize;
//	
//	private SampleMode sampleMode = SampleMode.CPU_TIME;
//	
//	public void setSnapshot(ThreadListSnapshot snapshot) {
//		if (this.snapshot == snapshot) {
//			return;
//		}
//		this.snapshot = snapshot;
//		validate();
//		markUpdateFlag(UF_PAINT);
//	}
//	
//	public void setSampleMode(SampleMode sampleMode) {
//		if (this.sampleMode == sampleMode) {
//			return;
//		}
//		this.sampleMode = sampleMode;
//		int oldItemHeight = itemHeight;
//		switch (sampleMode) {
//			case CPU_TIME : 
//				itemHeight = 3;
//				break;
//			case THREAD_STATES : 
//				itemHeight = 1;
//				break;
//		}
//		setViewY(itemHeight*viewY/oldItemHeight);
//		validate();
//		markUpdateFlag(UF_LAYOUT);
//	}
//
//	
//	
//	private void validate() {
//		if (snapshot == null) {
//			return;
//		}
//		List<ThreadMonitorInfo> infoList = snapshot.getInfoList();
//		int newViewHeight = infoList.size()*itemHeight;
//		setViewHeight(newViewHeight);
//		int nameColumnSize = 0;
//		for(ThreadMonitorInfo info : infoList) {
//			int nameLength = info.getThreadName().length();
//			if (nameLength > nameColumnSize) {
//				nameColumnSize = nameLength;
//			}
//		}
//		
//		if (nameColumnSize>50) {
//			nameColumnSize = 50;
//		}
//		
//		if (this.nameColumnSize != nameColumnSize) {
//			this.nameColumnSize = nameColumnSize;
//			markUpdateFlag(UF_PAINT);
//		}
//		
//	}
//
//	private void setViewHeight(int newViewHeight) {
//		if (viewHeight == newViewHeight)  {
//			return;
//		}
//		viewHeight = newViewHeight;
//		markUpdateFlag(UF_PAINT);
//	}
//
//	@Override
//	public int getViewHeight() {
//		return viewHeight;
//	}
//	
//	@Override
//	public int getViewWidth() {
//		return viewWidth;
//	}
//
//	public int getViewX() {
//		return viewX;
//	}
//	
//	public int getViewY() {
//		return viewY;
//	}
//
//	public void setViewY(int viewY) {
//		if (viewY<0) {
//			viewY = 0;
//		}
//		this.viewY = viewY;
//		markUpdateFlag(UF_PAINT);
//	}
//	
//	@Override
//	public int getPreferredHeight() {
//		return getViewHeight();
//	}
//	
//	@Override
//	public int getPreferredWidth() {
//		return getViewWidth();
//	}
//
//	@Override
//	public boolean onKeyEvent(KeyEvent keyEvent) {
//		KeyCode code = keyEvent.getCode();
//		if (code != null) {
//			switch(code) {
//				case CursorDown : {
//					if (highlightedItem+1<snapshot.getInfoList().size()) {
//						highlightedItem++;
//						markUpdateFlag(UF_PAINT);
//						ensureItemInView();
//					}
//					return true;
//				} 
//				
//				case CursorUp : {
//					if (highlightedItem>0) {
//						markUpdateFlag(UF_PAINT);
//						highlightedItem--;
//						ensureItemInView();
//					}
//					return true;
//				}
//				
//				case PgDown : {
//					pageDown();
//					return true;
//				}
//				case PgUp : {
//					pageUp();
//					return true;
//				}
//			}
//		}
//		
//		return false;
//	}
//	
//	private void pageDown() {
//		int itemsPerPage = getHeight()/itemHeight;
//		int newIndex = highlightedItem + itemsPerPage;
//		int itemCount = snapshot.getInfoList().size();
//		if (newIndex<itemCount) {
//			highlightedItem = newIndex;
//			setViewY(getViewY() + itemsPerPage * itemHeight);
//		} else {
//			if (highlightedItem+1 == itemCount) {
//				return;
//			}
//			highlightedItem = itemCount-1;
//			ensureItemInView();
//		}
//	}
//
//	private void pageUp() {
//		int itemsPerPage = getHeight()/itemHeight;
//		int newIndex = highlightedItem - itemsPerPage;
//		if (newIndex>=0) {
//			highlightedItem = newIndex;
//			setViewY(getViewY() - itemsPerPage * itemHeight);
//		} else {
//			if (highlightedItem == 0) {
//				return;
//			}
//			highlightedItem = 0;
//			ensureItemInView();
//		}
//	}
//
//	private void ensureItemInView() {
//		int viewTop = getViewY();
//		int viewBottom = viewTop+getHeight();
//
//		int topOfItem = highlightedItem*itemHeight;
//		int bottomOfItem = topOfItem + itemHeight;
//		
//		if (bottomOfItem>=viewBottom) {
//			setViewY(bottomOfItem-getHeight()+1);
//			viewTop = getViewY();
//		}
//		
//		if (topOfItem<viewTop) {
//			setViewY(topOfItem);
//		}
//	}
//
//	
//	@Override
//	public void paint(UpdateContext context) {
//		ThreadListSnapshot snapshot = this.snapshot;
//		
//		int topViewY = getViewY();
//		
//		int topIndex = topViewY/itemHeight;
//		int bottomViewY = topViewY+getHeight();
//		int bottomIndex = (bottomViewY+itemHeight-1)/itemHeight;
//		
//		
//		for(int curIndex=topIndex; curIndex<bottomIndex; curIndex++) {
//			ThreadMonitorInfo threadMonitorInfo = null;
//			if (snapshot!=null) {
//				List<ThreadMonitorInfo> infoList = snapshot.getInfoList();
//				if (curIndex>=0 && curIndex<infoList.size()) {
//					threadMonitorInfo = infoList.get(curIndex);
//				}
//			}
//			
//			if (threadMonitorInfo == null) {
//				int viewY = curIndex*itemHeight;
//				for(int idx=0; idx<itemHeight; idx++) {
//					context.moveCursor(0, viewY++);
//					context.clearToRight(context.getFormatDefault());
//				}
//			} else {
//
//				switch(sampleMode) {
//					case CPU_TIME :
//						paintCpuTime(context, curIndex, threadMonitorInfo);
//						break;
//						
//					case THREAD_STATES :
//						paintThreadState(context, curIndex, threadMonitorInfo);
//						break;
//				}
//			}
//		}
//	}
//
//	private void paintCpuTime(UpdateContext context, int curIndex, ThreadMonitorInfo threadMonitorInfo) {
//		int viewY = curIndex*itemHeight;
//		String name = threadMonitorInfo.getThreadName();
//		int maxLength = name.length();
//		if (maxLength>nameColumnSize) {
//			maxLength = nameColumnSize;
//		}
//		
//		Rectangle rectangle = new Rectangle(0, viewY, getViewWidth(), itemHeight);
//		context.clearRectangle(rectangle, new AnsiFormat(AnsiFormat.ANSI_BRIGHT_GREEN, -1, false));
//		
//		AnsiFormat formatFocus = context.pickSelectFormat(hasFocus(), curIndex==highlightedItem);
//		context.drawString(0, nameColumnSize, viewY, name, formatFocus, true);
////		paintText(context, formatFocus , 0, viewY, name, nameColumnSize);
//		context.clearLine(viewY+1, 0, nameColumnSize-1, formatFocus);
//		context.clearLine(viewY+2, 0, nameColumnSize-1, formatFocus);
//		
//		List<ThreadSample> samples = threadMonitorInfo.getSamples();
//		
//		int xOut = getWidth()-1;
//		int xSample = samples.size()-1;
//		
//		int xEnd = getViewX() + nameColumnSize;
//		
//		while(xSample>0 && xOut>xEnd) {
//			ThreadSample threadSample = samples.get(xSample);
//			if (threadSample == null) {
//				paintBar(context, xOut, viewY, 3, 0);
//			}
//			else {
//				int sample = threadSample.cpuPercentile;
//				int bar = 2*sample*3*9/1000;
//				paintBar(context, xOut, viewY, 3, bar);
//			}
//			xSample--;
//			xOut--;
//		}
//		
//	}
//	
//	private static final char BAR_CHARS[] = new char [] {
//		' ', '▁', '▂','▃','▄','▅','▆', '▇','█'
//	}; 
//
//	private void paintBar(UpdateContext context, int x, int y, int height, int sample) {
//		
//		AnsiFormat formatDefault = context.getFormatDefault();
//		int outY = y + height-1;
//		for(int subY=0; subY<height; subY++) {
//			int sampleLeft = sample - subY*9;
//			if (sampleLeft<0) {
//				sampleLeft = 0;
//			} else if (sampleLeft>=8) {
//				sampleLeft = 8;
//			}
//			context.moveCursor(x, outY);
//			context.writeChar(BAR_CHARS[sampleLeft], formatDefault);
//			outY--;
//			
//		}
//	}
//	
//	
//	private void paintThreadState(UpdateContext context, int curIndex, ThreadMonitorInfo threadMonitorInfo) {
//		int viewY = curIndex*itemHeight;
//		String name = threadMonitorInfo.getThreadName();
//		int maxLength = name.length();
//		if (maxLength>nameColumnSize) {
//			maxLength = nameColumnSize;
//		}
//		AnsiFormat formatFocus = context.pickSelectFormat(hasFocus(), curIndex==highlightedItem);
//		paintText(context, formatFocus , 0, viewY, name, nameColumnSize);
//		
//		List<ThreadSample> samples = threadMonitorInfo.getSamples();
//		
//		int xOut = getWidth()-1;
//		int xSample = samples.size()-1;
//		
//		int xEnd = getViewX() + nameColumnSize;
//		
//		while(xSample>0 && xOut>xEnd) {
//			ThreadSample threadSample = samples.get(xSample);
//			AnsiFormat format = formatStateNull;
//			char ch = ' ';
//			if (threadSample != null && threadSample.threadState!=null) {
//				switch(threadSample.threadState) {
//					case BLOCKED : format = formatStateBlocked; break;
//					case NEW : format = formatStateNew; break;
//					case RUNNABLE : format = formatStateRunnable; break;
//					case TERMINATED : format = formatStateTerminated; break;
//					case TIMED_WAITING : format = formatStateTimedWaiting; break;
//					case WAITING : format = formatStateWaiting; break;
//				}
//				if (threadSample.blockedPercentile>0) {
//					int level = threadSample.blockedPercentile/80;
//					if (level<0) {
//						level = 0;
//					} else if (level>9) {
//						level=9;
//					}
//					ch = (char) ('0' + level);
//				}
//				
//			}
//			context.moveCursor(xOut, viewY);
//			context.writeChar(ch, format);
//			xSample--;
//			xOut--;
//		}
//		
//	}	
//	
//
//	private void paintText(UpdateContext context, AnsiFormat format, int x, int y, String text, int maxSize) {
//		AnsiFormat pickFormat = null;
//		context.moveCursor(x, y);
//		for(int idx=0; idx<maxSize; idx++) {
//			char ch = ' ';
//			if (idx<text.length()) {
//				pickFormat = format;
//				ch = text.charAt(idx);
//			}
//			else {
//				pickFormat = context.getFormatDefault();
//			}
//			context.writeChar(ch, pickFormat);
//		}
//	}
//
//	
//	
//}
