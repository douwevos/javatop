package com.github.douwevos.javatop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class FilterConfig {

	private static final List<String> defaultExcludes = Arrays.asList("java", "jdk", "com.sun", "org.jboss",
			"org.wildfly", "__red", "io.swagger", "com.fasterxml", "org.hibernate", "sun", "io.", "org.infinispan",
			"org.postgresql", "liquibase", "org.apache", "org.xnio");


	private static final String CONFIG_FILE_NAME = "java-top-filters.conf";

	private static FilterConfig filterConfig = new FilterConfig();

	private final File configFile;
	
	private final List<FilterEntry> entries;

	private FilterConfig() {
		configFile = getConfigFile();
		List<FilterEntry> entries = parseConfig(configFile);
		if (entries == null || entries.isEmpty()) {
			entries = defaultExcludes.stream().map(e -> new FilterEntry(FilterType.PACKAGE_NAME_STARTS_WITH, false, e))
					.collect(Collectors.toList());
		}
		this.entries = new CopyOnWriteArrayList<>(entries);
	}

	private List<FilterEntry> parseConfig(File configFile) {
		if (configFile == null) {
			return null;
		}
		try {
			List<String> allLines = Files.readAllLines(configFile.toPath());
			return allLines.stream().map(this::parseLine).filter(Objects::nonNull).collect(Collectors.toList());

		} catch (IOException e) {
		}
		return null;
	}

	private FilterEntry parseLine(String line) {
		int idx = line.indexOf(':');
		if (idx < 0) {
			return null;
		}
		boolean inclusive = line.charAt(0) == '+';
		String typeText = line.substring(1, idx);
		String valueText = line.substring(idx + 1);
		FilterType filterType = FilterType.valueOf(typeText);
		return new FilterEntry(filterType, inclusive, valueText);
	}

	private void writeConfig() {
		if (configFile == null) {
			return;
		}
		try(PrintWriter out = new PrintWriter(configFile)) {
			for(FilterEntry entry : entries) {
				writeEntry(out, entry);
			}
			
		} catch (FileNotFoundException ignore) {
		}
	}
	
	private void writeEntry(PrintWriter out, FilterEntry entry) {
		String s = "" + (entry.inclusive ? '+' : '-')
				+entry.filterType.name()+':' + entry.value;
		out.println(s);
	}

	private File getConfigFile() {
		String userHome = System.getProperty("user.home");
		if (userHome != null) {
			File home = new File(userHome);
			if (home.exists()) {
				File homeConfig = new File(home, ".config");
				if (!homeConfig.exists()) {
					try {
						homeConfig.mkdirs();
					} catch(SecurityException ignore) {
					}
				}
				if (homeConfig.exists() && homeConfig.isDirectory()) {
					File configFile = new File(homeConfig, CONFIG_FILE_NAME);
					if (configFile.exists()) {
						return configFile;
					}
					try {
						FileWriter fileWriter = new FileWriter(configFile);
						fileWriter.flush();
						fileWriter.close();
						return configFile;
					} catch (IOException ignore) {
					}
				}
			}
		}

		int dotIdx = CONFIG_FILE_NAME.lastIndexOf('.');
		try {
			File tempFile = File.createTempFile(CONFIG_FILE_NAME.substring(0,dotIdx),CONFIG_FILE_NAME.substring(dotIdx+1));
			return tempFile;
		} catch (IOException ignore) {
		}
		return null;
	}

	public static FilterConfig getInstance() {
		return filterConfig;
	}

	public FilterEntry getFilterEntryAt(int index) {
		return entries.get(index);
	}
	
	public void setFilterEntryAt(int index, FilterEntry newEntry) {
		FilterEntry oldEntry = entries.get(index);
		if (newEntry.equals(oldEntry)) {
			return;
		}
		entries.set(index, newEntry);
		writeConfig();
	}

	public void removeFilterEntryAt(int index) {
		entries.remove(index);
		writeConfig();
	}
	
	public void addFilterEntryAt(int index, FilterEntry newEntry) {
		entries.add(index, newEntry);
		writeConfig();
	}



	public int filterEntryCount() {
		return entries.size();
	}

	
	public boolean filter(String className, String methodName) {
		Boolean keep = null;
		for(FilterEntry filterEntry : entries) {
			if (keep == null) {
				keep = filterEntry.inclusive ? Boolean.FALSE : Boolean.TRUE;
			}
			switch(filterEntry.filterType) {
				case FULLY_QUALIFIED_NAME_IS :
					if ((className+'.'+methodName).equals(filterEntry.value)) {
						return filterEntry.inclusive;
					}
					break;
					
				case METHOD_NAME_IS : 
					if ((methodName).equals(filterEntry.value)) {
						keep = filterEntry.inclusive;
					}
					break;
					
				case PACAKGE_NAME_CONTAINS :
					if ((className).contains(filterEntry.value)) {
						keep = filterEntry.inclusive;
					}
					break;
					
				case PACAKGE_NAME_IS :
					if ((className).equals(filterEntry.value)) {
						keep = filterEntry.inclusive;
					}
					break;
					
				case PACKAGE_NAME_STARTS_WITH :
					if ((className).startsWith(filterEntry.value)) {
						keep = filterEntry.inclusive;
					}
					break;
					
			}
		}
		return keep==null ? true : keep;
	}

	public enum FilterType {
		PACKAGE_NAME_STARTS_WITH, PACAKGE_NAME_CONTAINS, PACAKGE_NAME_IS, METHOD_NAME_IS, FULLY_QUALIFIED_NAME_IS
	}

	public static class FilterEntry {
		public final FilterType filterType;
		public final boolean inclusive;
		public final String value;

		public FilterEntry(FilterType filterType, boolean inclusive, String value) {
			this.filterType = filterType;
			this.inclusive = inclusive;
			this.value = value;
		}
		
		
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj instanceof FilterEntry) {
				FilterEntry that = (FilterEntry) obj;
				return that.inclusive == inclusive && that.filterType==filterType
						&& Objects.equals(that.value, value);
			}
			return false;
		}
	}




}
