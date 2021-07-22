package com.github.douwevos.terminal;

public class KeyEvent {

	private final KeyCode code;
	private final char key;
	
	private String text;
	
	public KeyEvent(char key) {
		this.key = key;
		switch(key) {
			case ' ' : code = KeyCode.Space; break;
			case 13 : code = KeyCode.Enter; break;
			case 127 : code = KeyCode.Backspace; break;
			default : code = null;
		}
	}
	
	public KeyEvent(KeyCode code) {
		this.key = 0;
		this.code = code;
	}

	
	public KeyEvent(String text) {
		key = 0;
		code = null;
		this.text = text;
	}

	public char getKey() {
		return key;
	}
	
	public KeyCode getCode() {
		return code;
	}
	

	public String toKeyText() {
		if (code != null) {
			return "<" + code.getLabel() + ">";
		}
		return "<" + key + ">";
	}
	
	
	public enum KeyCode {
		CursorUp("up"),
		CursorDown("down"),
		CursorLeft("left"),
		CursorRight("right"), 
		ShiftTab("shift-Tab"),
		Home("home"),
		End("end"),
		Insert("insert"),
		Delete("delete"),
		PgUp("page-up"),
		PgDown("page-down"),
		Escape("esc"),
		Space("space"),
		Enter("enter"),
		Backspace("backspace"),
		F1("F1"),
		F2("F2"),
		F3("F3"),
		F4("F4"),
		F5("F5"),
		F6("F6"),
		F7("F7"),
		F8("F8"),
		F9("F9"),
		F10("F10"),
		F11("F11"),
		F12("F12")
		;
		
		String label;
		
		KeyCode(String label) {
			this.label = label;
		}
		
		public String getLabel() {
			return label;
		}
	}


	@Override
	public String toString() {
		String keyCh = ""+key;
		if (key<32) {
			keyCh = "u:"+Integer.toHexString((int) key);
		}
		return "KeyEvent [code=" + code + ", key=" + keyCh + "("+(int) key+"), text=" + text + "]";
	}
	
	
}
