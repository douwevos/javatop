package com.github.douwevos.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class RawTerminal {

	private boolean isInRawMode;

	public RawTerminal() {
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}
	
	
	public synchronized boolean open() {
		if (isInRawMode) {
			return true;
		}
		if (runSttyCommand("raw") == 0) {
			isInRawMode = true;
		}
		return isInRawMode;
	}

	public synchronized void close() {
		if (!isInRawMode) {
			return;
		}
		if (runSttyCommand("sane") == 0) {
			isInRawMode = false;
			cursor(true);
		}
	}
	
	public void clearScreen() {
		System.out.print("\033[2J\033[1;1H");
	}
	
	public void cursorHome() {
		System.out.print("\033[1;1H");
	}

	
	public synchronized boolean echo(boolean on) {
		if (!isInRawMode) {
			return false;
		}
		return runSttyCommand(""+(on ? "" : "-")+"echo") == 0; 
	}
	
	public void cursor(boolean visible) {
		System.out.print("\033[?25"+(visible ? 'h' : 'l'));
	}

	
	public synchronized int[] size() {
		if (!isInRawMode) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for(int idx=0; idx<3; idx++) {
			int result = runSttyCommand("size", out);
			if (result == 0) {
				break;
			}
			if (idx==2) {
				return new int[] {20,20};
			}
			out = new ByteArrayOutputStream();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		String txt = new String(out.toByteArray());
		String[] split = txt.split(" ");
		int result[] = new int[2];
		result[0] = Integer.parseInt(split[0].trim());
		result[1] = Integer.parseInt(split[1].trim());
		return result;
	}
	
	
	private int runSttyCommand(String command) {
		String[] cmd = {"/bin/sh", "-c", "stty "+command+" </dev/tty"};
		try {
			int result = Runtime.getRuntime().exec(cmd).waitFor();
			return result;
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private int runSttyCommand(String command, ByteArrayOutputStream out) {
		String[] cmd = {"/bin/sh", "-c", "stty "+command+" </dev/tty"};
		try {
			byte data[] = new byte[512];
			Process exec = Runtime.getRuntime().exec(cmd);
			InputStream input = exec.getInputStream();
			while(true) {
				try { 
					int cnt = input.read(data);
					if (cnt<=0) {
						break;
					}
					out.write(data, 0, cnt);
				} catch(IOException e) {
					break;
				}
			}
			return exec.waitFor();
			
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}
		return -1;
	}


	
}
