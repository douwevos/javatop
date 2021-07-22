package com.github.douwevos.terminal.component;

import java.util.ArrayList;
import java.util.List;

import com.github.douwevos.terminal.AnsiFormat;
import com.github.douwevos.terminal.Rectangle;
import com.github.douwevos.terminal.UpdateContext;

public class PropertyList extends FaceComponentScrollable {

	private int maxKeySize;
	private int maxValueSize;
	private AnsiFormat keyFormat;
	private AnsiFormat valueFormat;
	
	private List<Property> properties = new ArrayList<>();

	public void setKeyFormat(AnsiFormat ansiFormat) {
		keyFormat = ansiFormat;
		markUpdateFlag(UF_PAINT);
	}
	
	public void add(Property property) {
		properties.add(property);
		maxKeySize = 0;
		maxValueSize = 0;
		markUpdateFlag(UF_PAINT);
	}
	
	public void removeAll() {
		properties.clear();
		maxKeySize = 0;
		maxValueSize = 0;
		markUpdateFlag(UF_PAINT);
	}
	
	@Override
	public int getViewHeight() {
		return properties.size();
	}
	
	@Override
	public int getViewWidth() {
		validate();
		if (maxKeySize==0 && maxValueSize==0) {
			return 0;
		}
		return maxKeySize + maxValueSize + 1;
	}
	
	@Override
	public void paint(UpdateContext context) {
		validate();

		for(int idx=0; idx<properties.size(); idx++) {
			Property property = properties.get(idx);
			AnsiFormat format = selectFormat(property.keyFormat, keyFormat, context.getFormatDefault());
			paintText(context, format, 0, idx, property.key, maxKeySize);
			context.writeChar(' ', context.getFormatDefault());
			format = selectFormat(property.valueFormat, valueFormat, context.getFormatDefault());
			paintText(context, format, maxKeySize+1, idx, property.value, maxValueSize);
			context.clearLine(idx, maxKeySize+1+maxValueSize, getWidth(), context.getFormatDefault());
		}

		Rectangle clearArea = new Rectangle(0, properties.size(), getWidth(), getHeight()-properties.size());
		context.clearRectangle(clearArea, context.getFormatDefault());
		
	}
	
	
	private void paintText(UpdateContext context, AnsiFormat format, int x, int y, String text, int maxSize) {
		AnsiFormat pickFormat = null;
		context.moveCursor(x, y);
		for(int idx=0; idx<maxSize; idx++) {
			char ch = ' ';
			if (idx<text.length()) {
				pickFormat = format;
				ch = text.charAt(idx);
			}
			else {
				pickFormat = context.getFormatDefault();
			}
			context.writeChar(ch, pickFormat);
		}
	}




	private AnsiFormat selectFormat(AnsiFormat formatA, AnsiFormat formatB, AnsiFormat formatC) {
		if (formatA != null) {
			return formatA;
		}
		if (formatB != null) {
			return formatB;
		}
		return formatC;
	}




	private void validate() {
		if ((maxKeySize!=0) || (maxValueSize!=0)) {
			return;
		}
		maxKeySize = 0;
		maxValueSize = 0;
		for(Property property : properties) {
			int keyLength = property.key.length();
			if (maxKeySize<keyLength) {
				maxKeySize = keyLength;
			}
			
			int valueLength = property.value.length();
			if (maxValueSize<valueLength) {
				maxValueSize = valueLength;
			}
		}
	}


	public static class Property {
		
		public final String key;
		public final AnsiFormat keyFormat;
		public final String value;
		public final AnsiFormat valueFormat;
		
		public Property(String key, String value) {
			this.key = key;
			this.keyFormat = null;
			this.value = value;
			this.valueFormat = null;
		}
		
		public Property(String key, AnsiFormat keyFormat, String value, AnsiFormat valueFormat) {
			this.key = key;
			this.keyFormat = keyFormat;
			this.value = value;
			this.valueFormat = valueFormat;
		}
		
	}
	
}
