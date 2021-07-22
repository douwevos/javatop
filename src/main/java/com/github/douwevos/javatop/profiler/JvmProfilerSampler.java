package com.github.douwevos.javatop.profiler;

import java.lang.Thread.State;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.github.douwevos.javatop.FilterConfig;
import com.github.douwevos.javatop.JvmService;

public class JvmProfilerSampler implements Runnable {

	private final JvmService jvmService;
	private final ProfileSnapshotObserver observer;

	private Map<Long, Long> lastThreadCpuTimeMap = new ConcurrentHashMap<>();

	private long totalMethodCpuTime; 

	private long lastMinimalCpuTime;

	private volatile String focusFqname;
	private volatile String invokersFqname;
	
	private Thread thread;
	
	private volatile boolean keepRunning = true;

	private volatile boolean pause = false;

	private Map<String, MethodCallInfo> methodCallInfoMap = new ConcurrentHashMap<>();

	private volatile ProfileSnapshot profileSnapshot;
	
	public JvmProfilerSampler(JvmService jvmService, ProfileSnapshotObserver observer) {
		this.jvmService = jvmService;
		this.observer = observer;
		thread = new Thread(this);
		thread.setName("Profile-Sampler");
		thread.start();
	}
	
	public ProfileSnapshot getProfileSnapshot() {
		return profileSnapshot;
	}
	
	public boolean togglePause() {
		pause = !pause;
		return pause;
	}
	
	public String getFocusFqname() {
		return focusFqname;
	}
	
	public void setFocusFqname(String focusFqname) {
		this.focusFqname = focusFqname;
	}
	
	public void setInvokersFqname(String invokersFqname) {
		this.invokersFqname = invokersFqname;
	}
	
	@Override
	public void run() {
		Set<Long> threadsRunningFocusMethod = new HashSet<>();
		FilterConfig filter = FilterConfig.getInstance();
		boolean nextIsBadSample = false;
		String lastInvokersName = null;
		while(keepRunning) {
			boolean doSleep = true;
			if (pause) {
				nextIsBadSample = true;
			}
			else {
				ThreadMXBean threadMXBeanProxy = jvmService.getThreadMXBeanProxy();
				if (threadMXBeanProxy != null) {
					String invokersName = invokersFqname;
					if (!Objects.equals(invokersName, lastInvokersName)) {
						lastInvokersName = invokersName;
						lastThreadCpuTimeMap.clear();
						methodCallInfoMap.clear();
						lastMinimalCpuTime = 0;
						totalMethodCpuTime = 0;
					}
					ThreadInfo[] allThreads = threadMXBeanProxy.dumpAllThreads(false, false);
					threadsRunningFocusMethod.clear();
					boolean didUpdate = false;
					
					for(ThreadInfo threadInfo : allThreads) {
						Long threadId = threadInfo.getThreadId();
						long threadCpuTime = threadMXBeanProxy.getThreadCpuTime(threadId);
						Long lastCpuTime = lastThreadCpuTimeMap.put(threadId, threadCpuTime);
						if (!nextIsBadSample && (lastCpuTime != null)) {
							long deltaCpuTime = (threadCpuTime - lastCpuTime);
	
							if (threadInfo.getThreadState() == State.RUNNABLE) {
								StackTraceElement[] stackTrace = threadInfo.getStackTrace();
								if (stackTrace.length > 0) {
									boolean invoked = false;
									for(StackTraceElement element : stackTrace) {
//										if (isReallySleeping(stElement)) {
//											break;
//										}
	
										String className = element.getClassName();
										if (!filter.filter(className, element.getMethodName())) {
											continue;
										}
										String fqMethodName = className + "." + element.getMethodName();
										
										if (invokersName != null) {
											if (fqMethodName.equals(invokersName)) {
												invoked = true;
												continue;
											}
											if (!invoked) {
												continue;
											}
										}
										
										if (Objects.equals(focusFqname, fqMethodName)) {
											threadsRunningFocusMethod.add(threadId);
											MethodCallInfoWithLines mci = null;
											MethodCallInfo methodCallInfo = methodCallInfoMap.get(fqMethodName);
											if (methodCallInfo instanceof MethodCallInfoWithLines) {
												mci = (MethodCallInfoWithLines) methodCallInfo;
											}
											else if (methodCallInfo == null) {
												mci = new MethodCallInfoWithLines(className, element.getMethodName());
												methodCallInfoMap.put(fqMethodName, mci);
											}
											else {
												mci = new MethodCallInfoWithLines(className, element.getMethodName());
												mci.cpuTime = methodCallInfo.cpuTime;
												methodCallInfoMap.put(fqMethodName, mci);
											}
											mci.addCpuTime(deltaCpuTime);
	
											int lineNumber = element.getLineNumber();
											if (lineNumber>=0) {
												mci.addLineHit(element.getLineNumber());
											}
										}
										else {	
											MethodCallInfo methodCallInfo = methodCallInfoMap.get(fqMethodName);
											if (methodCallInfo == null) {
												methodCallInfo = new MethodCallInfo(className, element.getMethodName());
												methodCallInfoMap.put(fqMethodName, methodCallInfo);
											}
											methodCallInfo.addCpuTime(deltaCpuTime);
									    }
										totalMethodCpuTime += deltaCpuTime;
										didUpdate = true;
										break;
									}
								}
							}
						}
					}
					
					nextIsBadSample = false;
					
					if (!threadsRunningFocusMethod.isEmpty()) {
						MethodCallInfo fci = focusFqname==null ? null : methodCallInfoMap.get(focusFqname);
						if (fci instanceof MethodCallInfoWithLines) {
							MethodCallInfoWithLines methodCallInfoFocus = (MethodCallInfoWithLines) fci;
							for(int idx=0; idx<20; idx++) {
								for(Long tid : threadsRunningFocusMethod) {
									ThreadInfo threadInfo = threadMXBeanProxy.getThreadInfo(tid);
									if (threadInfo != null) {
										StackTraceElement[] stackTrace = threadInfo.getStackTrace();
										for(StackTraceElement element : stackTrace) {
											String className = element.getClassName();
											String fqMethodName = className + "." + element.getMethodName();
											if (Objects.equals(focusFqname, fqMethodName)) {
												methodCallInfoFocus.addLineHit(element.getLineNumber());
												didUpdate = true;
											}
											
										}
									}
								}
								try {
									Thread.sleep(2);
								} catch (InterruptedException e) {
								}
								doSleep = false;
							}
						}
						
					}
	
					
					if (didUpdate) {
						List<MethodCallInfo> hotList = methodCallInfoMap.values().stream().filter(s -> s.cpuTime >= lastMinimalCpuTime).collect(Collectors.toList());
		
						Comparator<MethodCallInfo> c = (a,b) -> Long.compare(b.cpuTime, a.cpuTime);
						Collections.sort(hotList, c);
						List<MethodCallInfo> topList = new ArrayList<>(80);
						for(int idx=0; idx<80 && idx<hotList.size(); idx++) {
							topList.add(hotList.get(idx).copy());
						}
						if (topList.size()>=80) {
							lastMinimalCpuTime = topList.get(topList.size()-1).cpuTime;
						}
						
						MethodCallInfoWithLines methodCallInfoFocus = null;
						MethodCallInfo fci = focusFqname==null ? null : methodCallInfoMap.get(focusFqname);
						if (fci instanceof MethodCallInfoWithLines) {
							methodCallInfoFocus = (MethodCallInfoWithLines) fci;
						}
						
						profileSnapshot = new ProfileSnapshot(topList, totalMethodCpuTime, methodCallInfoFocus);
						if (observer != null) {
							observer.onNewSnapshot(profileSnapshot);
						}
					}
					
				}
			}
			
			try {

				if (doSleep) {
					Thread.sleep(70);
				}
				
			} catch (InterruptedException e) {
			}
		}
		
	}
	
	interface ProfileSnapshotObserver {
		public void onNewSnapshot(ProfileSnapshot snapshot);
	}
	


	public static class MethodCallInfo {

		protected final String className;
		protected final String methodName;
		
		protected long cpuTime;
		
		public MethodCallInfo(String className, String methodName) {
			this.className = className;
			this.methodName = methodName;
		}

		public MethodCallInfo copy() {
			MethodCallInfo copy = new MethodCallInfo(className, methodName);
			copy.cpuTime = cpuTime;
			return copy;
		}

		public void addCpuTime(Long deltaCpuTime) {
			cpuTime += deltaCpuTime;
		}
	}

	public static class MethodCallInfoWithLines extends MethodCallInfo {

		private int smallestLineNumber;
		private int linesFreq[];
		
		public MethodCallInfoWithLines(String className, String methodName) {
			super(className, methodName);
		}

		public MethodCallInfoWithLines copy() {
			MethodCallInfoWithLines copy = new MethodCallInfoWithLines(className, methodName);
			copy.cpuTime = cpuTime;
			if (linesFreq != null) {
				copy.linesFreq = new int[linesFreq.length];
				System.arraycopy(linesFreq, 0, copy.linesFreq, 0, linesFreq.length);
			}
			copy.smallestLineNumber = smallestLineNumber;
			return copy;
		}

		public int[] getLinesFreq() {
			return linesFreq;
		}
		
		public int getSmallestLineNumber() {
			return smallestLineNumber;
		}
		
		public void addLineHit(int lineNr) {
			if (linesFreq == null) {
				linesFreq = new int[1];
				smallestLineNumber = lineNr;
				linesFreq[0]++;
				return;
			}
			if (lineNr<smallestLineNumber) {
				int newFreq[] = new int[smallestLineNumber-lineNr +   linesFreq.length];
				System.arraycopy(linesFreq, 0, newFreq, smallestLineNumber-lineNr, linesFreq.length);
				newFreq[0]++;
				smallestLineNumber = lineNr;
				linesFreq = newFreq;
				return;
			}
			if (lineNr>=smallestLineNumber + linesFreq.length) {
				int newFreq[] = new int[1+lineNr-smallestLineNumber];
				System.arraycopy(linesFreq, 0, newFreq, 0, linesFreq.length);
				newFreq[lineNr-smallestLineNumber]++;
				linesFreq = newFreq;
				return;
			}
			linesFreq[lineNr-smallestLineNumber]++;
		}
	}

		
	
	
	public static class ProfileSnapshot {
		
		private final List<MethodCallInfo> methodCallInfos;
		private final long totalMethodCpuTime;
		private final MethodCallInfoWithLines focusMethod;
		
		public ProfileSnapshot(List<MethodCallInfo> methodCallInfos, long totalMethodCpuTime, MethodCallInfoWithLines focusMethod) {
			this.methodCallInfos = methodCallInfos;
			this.totalMethodCpuTime = totalMethodCpuTime;
			this.focusMethod = focusMethod;
		}

		public List<MethodCallInfo> getMethodCallInfos() {
			return methodCallInfos;
		}
		
		public long getTotalMethodCpuTime() {
			return totalMethodCpuTime;
		}
		
		public MethodCallInfoWithLines getFocusMethod() {
			return focusMethod;
		}
	}

	public void stop() {
		keepRunning = false;
		if (thread != null) {
			thread.interrupt();
		}
		try {
			thread.join(500);
		} catch (InterruptedException e) {
		}
	}
	
}
