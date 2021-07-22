package com.github.douwevos.javatop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class ProcessSampler implements Runnable {

	private final Observer observer;
	private Thread thread; 
	
	private volatile boolean keepRunning = true;
	private volatile Long processId;
	
	public ProcessSampler(Observer observer) {
		this.observer = observer;
		thread = new Thread(this);
		thread.setName("process-sampler");
		thread.start();
	}
	
	public void stop() {
		keepRunning = false;
		if (!thread.isAlive()) {
			return;
		}
		thread.interrupt();
		
		try {
			thread.join(500);
		} catch (InterruptedException e) {
		}
	}
	
	
	public void setProcessId(Long processId) {
		this.processId = processId;
	}
	
	@Override
	public void run() {

		
		while(keepRunning) {
			Stat stat = readStat(processId);
			PsInfo psInfo = readPsInfo(processId);
			
			
			if (observer != null) {
				observer.onNewStat(stat, psInfo);
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
			
		}
		
	}

	private PsInfo readPsInfo(Long processId) {
		if (processId == null) {
			return null;
		}
		
		
		File psExe = new File("/usr/bin/ps");
		if (!psExe.exists()) {
			psExe = new File("/bin/ps");
			if (!psExe.exists()) {
				return null;
			}
		}
		
		try {
			Process process = Runtime.getRuntime().exec(new String[]{
					psExe.getPath()
					, "-q"
					, ""+processId
					, "-o"
					, "%cpu,%mem,uid,stime,time,user"
			});
			InputStream input = process.getInputStream();
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(input));
			String lastLine = null;
			while(true) {
				String readLine = reader.readLine();
				if (readLine == null) {
					break;
				}
				lastLine = readLine;
			}
			reader.close();

			
			if (lastLine == null) {
				return null;
			}
			List<String> items = new ArrayList<>();
			StringTokenizer st = new StringTokenizer(lastLine);
			while(st.hasMoreElements()) {
				String t = (String) st.nextElement();
				if (t.isBlank()) {
					continue;
				}
				items.add(t);
			}
			if (items.size()>=6) {
				PsInfo result = new PsInfo();
				result.percentageCpu = items.get(0);
				result.percentageMem = items.get(1);
				result.uid = items.get(2);
				result.startTime = items.get(3);
				result.upTime = items.get(4);
				result.user = items.get(5);

//				result.uid = "";
//				result.user = lastLine;
				return result;
			}
			
		} catch (IOException ignore) {
		}
		
		
		return null;
	}

	private Stat readStat(Long processId) {
		if (processId == null) {
			return null;
		}
		
		File fileStat = new File("/proc/"+processId+"/stat");
		if (!fileStat.exists()) {
			return null;
		}
		try {
			String statString = Files.readString(fileStat.toPath());
			String[] entries = statString.split(" ");
			if (entries.length>27) {
				Stat stat = new Stat();
				stat.pid = entries[0];
				stat.command = entries[1];
				stat.state =  entries[2];
				stat.parentPid =  entries[3];
				stat.processGroupId =  entries[4];
				stat.sessionId =  entries[5];
				stat.ttyNr =  entries[6];
				stat.tpgId =  entries[7];
				stat.flags =  entries[8];
				stat.minorFaults =  entries[9];
				stat.childrenMinorFaults =  entries[10];
				stat.majorFaults =  entries[11];
				stat.childrenMajorFaults =  entries[12];
				stat.userModeTime =  entries[13];
				stat.scheduledTime =  entries[14];
				stat.childrenUserModeTime =  entries[15];
				stat.childrenScheduledTime =  entries[16];
				stat.priority =  entries[17];
				stat.nice =  entries[18];
				stat.numberOfThreads =  entries[19];
				stat.itRealValue =  entries[20];
				stat.startTime =  entries[21];
				stat.virtualMemorySize =  entries[22];
				stat.residentSetSize =  entries[23];
				stat.residentSetSizeLimit =  entries[24];
				stat.startCode =  entries[25];
				stat.endCode =  entries[26];
				stat.startStack =  entries[27];
				return stat;
			}
		} catch (IOException e) {
		}
		return null;
	}

	public static class Stat {
		public String pid;
		public String command;
		public String state;
		public String parentPid;
		public String processGroupId;
		public String sessionId;
		public String ttyNr;
		public String tpgId;
		public String flags;
		public String minorFaults;
		public String childrenMinorFaults;
		public String majorFaults;
		public String childrenMajorFaults;
		public String userModeTime;
		public String scheduledTime;
		public String childrenUserModeTime;
		public String childrenScheduledTime;
		public String priority;
		public String nice;
		public String numberOfThreads;
		public String itRealValue;
		public String startTime;
		public String virtualMemorySize;
		public String residentSetSize;
		public String residentSetSizeLimit;
		public String startCode;
		public String endCode;
		public String startStack;
	}
	
	public static class PsInfo {
		public String percentageCpu;
		public String percentageMem;
		public String uid;
		public String startTime;
		public String upTime;
		public String user;
	}
	
	
	public interface Observer {
		void onNewStat(Stat stat, PsInfo psInfo);
	}
}
