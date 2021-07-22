package com.github.douwevos.terminal;

import java.io.Console;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.douwevos.terminal.KeyEvent.KeyCode;
import com.github.douwevos.terminal.component.Face;
import com.github.douwevos.terminal.component.KeyAction;
import com.github.douwevos.terminal.component.Label;
import com.github.douwevos.terminal.component.Scrollable;

public class TerminalWindow  {

	private final RawTerminal rawTerminal;
	private final KeyAction actionQuit = new KeyAction("<q>", new Label("quit", new AnsiFormat(-1, AnsiFormat.ANSI_RED, false)), this::doQuit);
	private Face face;
	private Face dialog;
	private Face focus;
	
	private int loopSequence;
	
	private AnsiFormat baseFormat = new AnsiFormat(-1, -1, false);
	private AnsiFormat formatFocusBorder = new AnsiFormat(-1, AnsiFormat.ANSI_BRIGHT_WHITE, false);
	private AnsiFormat formatSelect = new AnsiFormat(AnsiFormat.ANSI_BLUE, AnsiFormat.ANSI_GRAY, false);
	private AnsiFormat formatSelectFocus = new AnsiFormat(AnsiFormat.ANSI_BRIGHT_MAGENTA, AnsiFormat.ANSI_BLACK, false);
	
	private int columns, rows;
	private boolean keepRunning = true;
	
	private List<TerminalLine> lines = new ArrayList<>();
	
	private List<TerminalLine> cache = new ArrayList<>();
	
	private List<Runnable> queue = new ArrayList<>();
	
	private ConsoleReader consoleReader = new ConsoleReader();
	private Thread readerThread;
	
	
	public TerminalWindow(RawTerminal rawTerminal) {
		this.rawTerminal = rawTerminal;
		readerThread = new Thread(consoleReader);
		readerThread.start();
	}
	
	public void flush() {
		
		int recursiveUpdateFlags = face.getRecursiveUpdateFlags();
		
		if ((recursiveUpdateFlags & Face.UF_LAYOUT) != 0) {
			face.updateLayout(0,0, columns, rows);
		}
		recursiveUpdateFlags = face.getRecursiveUpdateFlags();
		
		if ((recursiveUpdateFlags & (Face.UF_CLEAR | Face.UF_PAINT)) !=0) {
			ClipRectangles clipScreen = new ClipRectangles(new Rectangle(0, 0, columns, rows));
			if (dialog != null) {
				Rectangle screenRectDialog = dialog.getOnScreenRect();
				clipScreen = clipScreen.clipExclude(screenRectDialog);
			}
			if (clipScreen != null) {
				UpdateContext updateContext = new UpdateContextFace(face, clipScreen);
				face.update(updateContext);
			}
		}
		
		if (dialog != null) {
			recursiveUpdateFlags = dialog.getRecursiveUpdateFlags();
			
			if ((recursiveUpdateFlags & Face.UF_LAYOUT) != 0) {
				dialog.updateLayout(dialog.getX(),dialog.getY(), dialog.getWidth(), dialog.getHeight());
			}
			recursiveUpdateFlags = dialog.getRecursiveUpdateFlags();

			if ((recursiveUpdateFlags & (Face.UF_CLEAR | Face.UF_PAINT)) !=0) {
				Clip clipScreen = new ClipRectangles(new Rectangle(0, 0, columns, rows));
				clipScreen = clipScreen.clipIntersect(dialog.getOnScreenRect());
				if (clipScreen != null) {
					UpdateContext updateContext = new UpdateContextFace(face, clipScreen);
					dialog.update(updateContext);
				}
			}

		}

		if (cache.size()!=lines.size()) {
			cache.clear();
		}
		rawTerminal.cursorHome();
		System.out.print("\033[0m");
		AnsiFormat format = baseFormat;
		System.out.print(""+baseFormat.getFormatText());
		for(int idx=0; idx<lines.size(); idx++) {
			TerminalLine terminalLine = lines.get(idx);
			TerminalLine cacheLine = null;
			if (idx<cache.size()) {
				cacheLine = cache.get(idx);
				if (cacheLine.columnCount()!=terminalLine.columnCount()) {
					cacheLine = null;
				}
			}
			format = flushLine(terminalLine, cacheLine, idx, format);
			
			if (cacheLine == null) {
				cacheLine = terminalLine.copy();
				if (idx<cache.size()) {
					cache.set(idx, cacheLine);
				} else {
					cache.add(cacheLine);
				}
			}
		}
			
	}
	
	
	private AnsiFormat flushLine(TerminalLine terminalLine, TerminalLine cache, int row, AnsiFormat format) {
		int columnCount = terminalLine.columnCount();
		AnsiFormat defaultBgFormat = new AnsiFormat(-1, -1, false);
		
		boolean cursorDirty = true;
		
		for(int column=0; column<columnCount; column++) {
			char ch = terminalLine.charAt(column);
			AnsiFormat nextFormat = terminalLine.formatAt(column);
			if (cache!=null) {
				if (cache.charAt(column) == ch && Objects.equals(cache.formatAt(column), nextFormat)) {
					cursorDirty = true;
					continue;
				}
				cache.setCharAt(ch, nextFormat, column);
			}
			
			if (cursorDirty) {
				System.out.print("\033["+(row+1)+";"+(column+1)+"H");
			}
			
			
			if (!Objects.equals(format, nextFormat)) {
				format = nextFormat;
				System.out.print(format==null ? defaultBgFormat.getFormatText() : format.getFormatText());
			}
			System.out.print(ch);
			cursorDirty = false;
		}
		return format;
	}
	
	private void nextFocus() {
		List<Face> focusElements = new ArrayList<>();
		Face of = dialog==null ? face : dialog;
		of.enlistFocusElements(focusElements);
		if (focusElements.isEmpty()) {
			setFocus(null);
			return;
		}
		int index = 0;
		if (focus != null) {
			index = focusElements.indexOf(focus);
			index++;
		}
		if (index>=focusElements.size()) {
			index = 0;
		}
		setFocus(focusElements.get(index));
	}
	
	private void firstFocus() {
		List<Face> focusElements = new ArrayList<>();
		Face of = dialog==null ? face : dialog;
		of.enlistFocusElements(focusElements);
		if (!focusElements.isEmpty()) {
			if (focus==null || !focusElements.contains(focus)) {
				setFocus(focusElements.get(0));
			}
		} 
		else {
			setFocus(null);
			focus = null;
		}
		
	}

	
	private void setFocus(Face newFocus) {
		if (newFocus == focus) {
			return;
		}
		if (focus!=null) {
			focus.onFocusChanged(false);
			focus.markUpdateFlag(Face.UF_PAINT);
		}
		focus = newFocus;
		if (focus!=null) {
			focus.onFocusChanged(true);
			focus.markUpdateFlag(Face.UF_PAINT);
		}
		rebuiltKeyActionList();
	}

	private void rebuiltKeyActionList() {
		KeyActionCollector collector = new KeyActionCollector();
		collector.add(actionQuit);
		if (face != null) {
			face.enlistKeyActions(collector);
		}
		collector.notifyObservers();
		
		
		if (dialog!=null) {
			collector = new KeyActionCollector();
			if (dialog != null) {
				dialog.enlistKeyActions(collector);
			}
			collector.notifyObservers();
		}
	}

	public void setFace(Face face) {
		this.face = face;
		this.dialog = null;
		face.updateLayout(0, 0, columns, rows);
		face.markUpdateFlag(Face.UF_PAINT);
		firstFocus();			
		flush();
	}
	
	public void setDialog(Dialog face) {
		if (this.dialog == face) {
			return;
		}
		this.dialog = face;
		if (dialog != null) {
			dialog.updateLayout(dialog.getX(), dialog.getY(), dialog.getWidth(), dialog.getHeight());
			dialog.markUpdateFlag(Face.UF_PAINT);
		}
		firstFocus();
	}
	
	public void validateSize() {
		if ((loopSequence%10) !=0) {
			return;
		}
		int[] size = rawTerminal.size();
		if (size == null) {
			rows = 40;
			columns = 80;
		} else {
			if ((size[0] == rows) && (size[1] == columns)) {
				return;
			}
			
			rows = size[0];
			columns = size[1];
		}
		lines = new ArrayList<>();
		for(int idx=0; idx<rows; idx++) {
			lines.add(new TerminalLine(columns, baseFormat));
		}
		
		if (face != null) {
			face.updateLayout(0, 0, columns, rows);
		}
		if (dialog != null) {
			dialog.markUpdateFlag(Face.UF_CLEAR);
		}
	}

	public int getRows() {
		return rows;
	}
	
	class UpdateContextFace extends UpdateContext {
		
		private final Face face;
		private final Clip clip;
		
		public UpdateContextFace(Face face, Clip clip) {
			this.face = face;
			this.clip = clip;
		}
		
		public Cursor getOnScreenLocation(int column, int row) {
			column += face.getX();
			row += face.getY();
			
			if (face instanceof Scrollable) {
				Scrollable scrollable = (Scrollable) face;
				column -= scrollable.getViewX();
				row -= scrollable.getViewY();
			}
			return new Cursor(column, row);
		}
		
		public void writeChar(char ch, AnsiFormat format) {
			Cursor location = getOnScreenLocation(cursorX, cursorY);
			cursorX++;
			if (!clip.inside(location.column, location.row)) {
				return;
			}
			TerminalLine terminalLine = lines.get(location.row);
			terminalLine.setCharAt(ch, format, location.column);
		}


		@Override
		public void writeString(String text, AnsiFormat format) {
			if (text == null) {
				return;
			}
			Cursor location = getOnScreenLocation(cursorX, cursorY);
			int length = text.length();
			cursorX += length;
			
			Rectangle textOSRect = new Rectangle(location.column, location.row, length, 1);
			if (!clip.intersects(textOSRect)) {
				return;
			}
			
			TerminalLine terminalLine = lines.get(location.row);
			
			int column = location.column;
			for(int off=0; off<length; off++) {
				if (clip.inside(column, location.row)) {
					terminalLine.setCharAt(text.charAt(off), format, column);
				}
				column++;
			}
		}
		
		@Override
		public void clearLine(int y, int left, int right, AnsiFormat format) {
			if (left>right) {
				return;
			}
			
			int row = y;
			if (face instanceof Scrollable) {
				Scrollable scrollable = (Scrollable) face;
				left -= scrollable.getViewX();
				right -= scrollable.getViewX();
				row -= scrollable.getViewY();
			}

			if ((right<0) || (row<0)) {
				return;
			}
			int width = face.getWidth();
			int height = face.getHeight();
			if ((left>=width) || (row>=height)) {
				return;
			}
			
			row += face.getY();
			
			if (row<0 || row>=lines.size()) {
				return;
			}

			
			if (left<0) {
				left = 0;
			}
			if (right>=width) {
				right = width-1; 
			}
			left += face.getX();
			right += face.getX();
			TerminalLine terminalLine = lines.get(row);

			if (left<0) {
				left = 0;
			}
			if (right>=terminalLine.columnCount()) {
				right = terminalLine.columnCount()-1;
			}
			
			while(left<=right) {
				terminalLine.setCharAt(' ', format, left);
				left++;
			}
			
		}


		
		@Override
		public UpdateContext withFace(Face face) {
			return new UpdateContextFace(face, clip.clipIntersect(face.getOnScreenRect()));
		}
		
		@Override
		public Face getFace() {
			return face;
		}
		
		@Override
		public AnsiFormat getFormatDefault() {
			return baseFormat;
		}

		@Override
		public AnsiFormat getFormatFocusBorder() {
			return formatFocusBorder;
		}

		@Override
		public AnsiFormat getFormatSelect() {
			return formatSelect;
		}
		
		@Override
		public AnsiFormat getFormatSelectFocus() {
			return formatSelectFocus;
		}
		
	}

	public boolean keepRunning() {
		return keepRunning;
	}

	public void invokeLater(Runnable runnable) {
		synchronized (queue) {
			queue.add(runnable);
		}
	}
	
	private void handleLaterInvokes() {
		List<Runnable> runNow = null;
		synchronized (queue) {
			if (!queue.isEmpty()) {
				runNow = new ArrayList<>(5);
				int z = queue.size()>5 ? 5 : queue.size();
				List<Runnable> subList = queue.subList(0, z);
				runNow.addAll(subList);
				subList.clear();
			}
		}
		
		if (runNow != null) {
			for(Runnable r : runNow) {
				try {
					r.run();
				} catch(Throwable t) {
					t.printStackTrace();
					// TODO show user error or log error
				}
			}
		}
	}

	private void doQuit() {
		keepRunning = false;
		consoleReader.stop();
		readerThread.interrupt();
	}
	
	public void mainLoop() {
		if (!keepRunning) {
			return;
		}
		handleLaterInvokes();
		validateSize();
		loopSequence++;
		KeyEvent keyEvent = consoleReader.pollKeyEvent(50);
		if (keyEvent != null) {
			if (keyEvent.getKey() == '\t') {
				nextFocus();
			} else {
				if (focus != null) {
					if (focus.onKeyEvent(keyEvent)) {
						return;
					}
				}
				if (dialog != null) {
					if (dialog.onKeyEvent(keyEvent)) {
						return;
					}
				}
				if (face != null) {
					face.onKeyEvent(keyEvent);
				}
			}
		}
	}
	
	private static class ConsoleReader implements Runnable {
		
		private boolean keepRunning = true;
		
		private char buf[] = new char[32];
		
		private List<KeyEvent> keys = new ArrayList<>();
		
		public void run() {
			Console console = System.console();
			Reader reader = console.reader();
			int bufSize = 0;
			while(keepRunning) {
				
				try {
					int readCnt = reader.read(buf, bufSize, buf.length-bufSize);
					if (readCnt<0) {
						break;
					}
					bufSize += readCnt;
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (bufSize>0) {
					int rest = scanForKeys(bufSize);
					if (rest>0) {
						if (rest<bufSize) {
							System.arraycopy(buf, rest, buf, 0, buf.length-rest);
						}
						bufSize-=rest;
					}
				}
			}
		}

		public void stop() {
			keepRunning = false;
		}

		private int scanForKeys(int bufSize) {
			int scan = 0;
			while(scan<bufSize) {
				if (buf[scan] == 27) {
					if (scan+5<=bufSize) {
						KeyEvent event = null;
						String c = ""+buf[scan+1]+buf[scan+2]+buf[scan+3]+buf[scan+4];
						switch(c) {
							case "[11~" : event = new KeyEvent(KeyCode.F1); break;
							case "[12~" : event = new KeyEvent(KeyCode.F2); break;
							case "[13~" : event = new KeyEvent(KeyCode.F3); break;
							case "[14~" : event = new KeyEvent(KeyCode.F4); break;
							case "[15~" : event = new KeyEvent(KeyCode.F5); break;
							case "[17~" : event = new KeyEvent(KeyCode.F6); break;
							case "[18~" : event = new KeyEvent(KeyCode.F7); break;
							case "[19~" : event = new KeyEvent(KeyCode.F8); break;
							case "[20~" : event = new KeyEvent(KeyCode.F9); break;
							case "[21~" : event = new KeyEvent(KeyCode.F10); break;
							case "[23~" : event = new KeyEvent(KeyCode.F11); break;
							case "[24~" : event = new KeyEvent(KeyCode.F12); break;
						}
						
						if (event != null) {
							scan += 5;
							postKeyEvent(event);
							continue;
						}
					}
					if (scan+4<=bufSize) {
						KeyEvent event = null;
						String c = ""+buf[scan+1]+buf[scan+2]+buf[scan+3];
						switch(c) {
							case "[1~" : event = new KeyEvent(KeyCode.Home); break;
							case "[2~" : event = new KeyEvent(KeyCode.Insert); break;
							case "[3~" : event = new KeyEvent(KeyCode.Delete); break;
							case "[4~" : event = new KeyEvent(KeyCode.End); break;
							case "[5~" : event = new KeyEvent(KeyCode.PgUp); break;
							case "[6~" : event = new KeyEvent(KeyCode.PgDown); break;
							case "[7~" : event = new KeyEvent(KeyCode.Home); break;
							case "[8~" : event = new KeyEvent(KeyCode.End); break;
						}
						
						if (event != null) {
							scan += 4;
							postKeyEvent(event);
							continue;
						}
					}
					if (scan+3<=bufSize) {
						KeyEvent event = null;
						String c = ""+buf[scan+1]+buf[scan+2];
						switch(c) {
							case "[A" : event = new KeyEvent(KeyCode.CursorUp); break;
							case "[B" : event = new KeyEvent(KeyCode.CursorDown); break;
							case "[C" : event = new KeyEvent(KeyCode.CursorRight); break;
							case "[D" : event = new KeyEvent(KeyCode.CursorLeft); break;
							case "[Z" : event = new KeyEvent(KeyCode.ShiftTab); break;
							case "[H" : event = new KeyEvent(KeyCode.Home); break;
							case "[F" : event = new KeyEvent(KeyCode.End); break;

							case "OP" : event = new KeyEvent(KeyCode.F1); break;
							case "OQ" : event = new KeyEvent(KeyCode.F2); break;
							case "OR" : event = new KeyEvent(KeyCode.F3); break;
							case "OS" : event = new KeyEvent(KeyCode.F4); break;

						}
						if (event != null) {
							scan+=3;
							postKeyEvent(event);
							continue;
						}
					}
					
					if (scan+1<=bufSize) {
						scan++;
						postKeyEvent(new KeyEvent(KeyCode.Escape));
					} else {
						break;
					}
				}
				else {
					postKeyEvent(new KeyEvent(buf[scan++]));
				}
			}
			
			return scan;
		}

		public synchronized void postKeyEvent(KeyEvent event) {
			keys.add(event);
			notifyAll();
		}
		
		public synchronized KeyEvent pollKeyEvent(long timeOut) {
			if (keys.isEmpty()) {
				try {
					this.wait(timeOut);
				} catch (InterruptedException e) {
				}
				if (keys.isEmpty()) {
					return null;
				}
			}
			return keys.remove(0);
		}
	}
	
}
