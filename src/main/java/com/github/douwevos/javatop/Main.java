package com.github.douwevos.javatop;

import com.github.douwevos.terminal.RawTerminal;
import com.github.douwevos.terminal.TerminalWindow;

public class Main {

	
	public static void main(String[] args) {
		Main main = new Main();
		main.run(args);
	}
	
	
	private void run(String[] args) {
		parseArgs(args);
		
		RawTerminal rawTerminal = new RawTerminal();
		
		try {
			rawTerminal.open();
			rawTerminal.echo(false);
			rawTerminal.cursor(false);
			rawTerminal.clearScreen();

			
			TerminalWindow terminalWindow = new TerminalWindow(rawTerminal);
			
			JvmListController jvmListController = new JvmListController(terminalWindow);
			jvmListController.show();
			
			while(terminalWindow.keepRunning()) {
				terminalWindow.mainLoop();
				terminalWindow.flush();
			}
			
			
			
			System.out.print("\033["+terminalWindow.getRows()+";1H");
		} finally {
			

			
			rawTerminal.close();
		}
		
		
		System.out.println("");
		System.out.println("Bye !!");
		System.exit(0);
		
	}


	private void parseArgs(String[] args) {
		
	}
	
}
